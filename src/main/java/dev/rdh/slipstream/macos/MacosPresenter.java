package dev.rdh.slipstream.macos;

import dev.rdh.slipstream.GlPresenter;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

public final class MacosPresenter implements GlPresenter {
    private final long context, window;
    private final MacosSurfacePool pool;
    private final Thread owner = Thread.currentThread();
    private final int[] textures = new int[MacosSurfacePool.SIZE];
    private final int[] framebuffers = new int[MacosSurfacePool.SIZE];
    private int sourceFramebuffer;
    private int width, height;
    private final ByteBuffer mask = BufferUtils.createByteBuffer(4);
    private final float[] clear = new float[4];
    private boolean closed;

    // based off vibes
    private static final long STRICT_ABOVE = 8_300_000L, OVERLAP_BELOW = 5_000_000L;
    private static final double MAX_STRICT_COST = 1.1;
    private static final int SETTLE = 8, TRIAL = 32;
    private static final long FIRST_RETRY = 2_000_000_000L, LAST_RETRY = 16_000_000_000L;
    private boolean strict = true, trial;
    private int sinceSwitch, strictCount, overlapCount;
    private long lastFrame, idle, work, retryAt, retryDelay = FIRST_RETRY;
    private final long[] strictFrames = new long[TRIAL - SETTLE], overlapFrames = new long[TRIAL - SETTLE];

    // direct presentation: the game framebuffer renders straight into a surface (see presentFramebuffer(..., direct))
    private static final boolean DIRECT = Boolean.getBoolean("slipstream.direct");
    private int attached = -1;          // surface attached to the game framebuffer, or -1
    private int attachedFramebuffer;
    private int ownTexture;             // the framebuffer's own colour texture, given back when leaving direct mode
    private boolean attachedFresh;      // the attached surface holds a finished frame that hasn't been shown
    private boolean ownTextureStale;    // the newest frame is in a surface, not in the framebuffer's own texture
    private int lastShown = -1;

    public MacosPresenter(long window) {
        if (window == 0) throw new IllegalArgumentException("Missing Cocoa window");
        context = Macos.currentContext();
        if (context == 0) throw new IllegalStateException("No current OpenGL context");
        this.window = window;
        pool = new MacosSurfacePool(window);
    }

    @Override
    public void present(int texture, int width, int height) {
        if (texture == 0) throw new IllegalArgumentException("Invalid texture");
        present(texture, 0, width, height);
    }

    @Override
    public void presentFramebuffer(int framebuffer, int width, int height) {
        presentFramebuffer(framebuffer, width, height, false);
    }

    @Override
    public void presentFramebuffer(int framebuffer, int width, int height, boolean direct) {
        if (framebuffer < 0) throw new IllegalArgumentException("Invalid framebuffer");
        if (!DIRECT || !Macos.CONTENTS_OPAQUE || framebuffer == 0 || (!direct && attached < 0)) {
            present(0, framebuffer, width, height);
            return;
        }
        presentDirect(framebuffer, width, height, direct);
    }

    /**
     * Direct presentation. The game framebuffer's colour attachment is one of the pool's surfaces, so the frame that was
     * just rendered is already presentable: no copy. A surface is shown only when the compositor has released another
     * one to attach next; otherwise the frame is dropped and the next one is rendered into the same surface, so at
     * high frame rates the attachment changes about once per refresh rather than every frame. Frames rendered by OpenGL
     * are bottom-up, so they are shown mirrored (the copy path flips while copying instead).
     */
    private void presentDirect(int framebuffer, int width, int height, boolean direct) {
        checkContext();
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid frame");
        int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        boolean copy = false;
        try {
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            int current = glGetFramebufferAttachmentParameteri(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
            boolean ours = attached >= 0 && framebuffer == attachedFramebuffer && current == textures[attached];
            if (attached >= 0 && (!ours || this.width != width || this.height != height)) {
                // the game replaced its attachment or resized: our surface no longer holds this frame
                detach();
                ours = false;
            }
            if (!ours) {
                int next = direct && this.width == width && this.height == height ? pool.free(i -> true) : -1;
                if (next < 0) {
                    copy = true;
                    return;
                }
                // entering direct mode: this frame is in the framebuffer's own texture; drop it and render the next
                // one into a surface (copying it would take the free surface the attachment needs)
                ownTexture = current;
                attachedFramebuffer = framebuffer;
                attach(next);
                glFlush();
                return;
            }
            measure();
            ownTextureStale = true;
            if (!direct) {
                // leaving direct mode: show this last direct frame, then give the framebuffer its own texture back
                long frame = submit();
                pool.show(attached, true);
                lastShown = attached;
                detach();
                finish(frame);
                return;
            }
            int held = attached;
            int next = pool.free(i -> i != held);
            if (next < 0) {
                // the compositor still holds the other surfaces: drop this frame, the next one overwrites it
                attachedFresh = true;
                glFlush();
                return;
            }
            long frame = submit();
            pool.show(held, true);
            lastShown = held;
            attach(next);
            finish(frame);
        } finally {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
            if (copy) present(0, framebuffer, width, height);
        }
    }

    /** Attach surface {@code index} as the colour target of the framebuffer bound to GL_FRAMEBUFFER. */
    private void attach(int index) {
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 0x84F5, textures[index], 0);
        attached = index;
        attachedFresh = false;
    }

    /** Give the attached framebuffer its own colour texture back. Leaves GL_FRAMEBUFFER bound to it. */
    private void detach() {
        if (attached < 0) return;
        glBindFramebuffer(GL_FRAMEBUFFER, attachedFramebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ownTexture, 0);
        attached = -1;
        attachedFresh = false;
    }

    @Override
    public void syncFramebuffer() {
        if (!ownTextureStale || ownTexture == 0 || closed || Thread.currentThread() != owner) return;
        int source = attachedFresh && attached >= 0 ? attached : lastShown;
        if (source < 0) return;
        int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            if (sourceFramebuffer == 0) sourceFramebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, sourceFramebuffer);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ownTexture, 0);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffers[source]);
            glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            ownTextureStale = false;
        } finally {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
        }
    }

    private void present(int texture, int framebuffer, int width, int height) {
        checkContext();
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid frame");
        if (framebuffer != 0 && framebuffer == attachedFramebuffer) ownTextureStale = false;
        int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int rectangle = glGetInteger(0x84F6);
        boolean scissor = glIsEnabled(GL_SCISSOR_TEST);
        boolean srgb = glIsEnabled(GL_FRAMEBUFFER_SRGB);
        try {
            measure();
            if (this.width != width || this.height != height) resize(width, height);
            int next = pool.free(i -> true);
            if (next < 0) {
                finish(submit());
                return;
            }
            if (texture != 0) {
                if (sourceFramebuffer == 0) sourceFramebuffer = glGenFramebuffers();
                framebuffer = sourceFramebuffer;
            }
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            if (texture != 0) glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
            requireFramebuffer(GL_FRAMEBUFFER);
            glDisable(GL_SCISSOR_TEST);
            glDisable(GL_FRAMEBUFFER_SRGB);
            if (!Macos.CONTENTS_OPAQUE) {
                // fallback: the compositor blends by the frame's alpha, so overwrite it in the source, at some cost
                glGetBooleanv(GL_COLOR_WRITEMASK, mask);
                glGetFloatv(GL_COLOR_CLEAR_VALUE, clear);
                glColorMask(false, false, false, true);
                glClearColor(0, 0, 0, 1);
                glClear(GL_COLOR_BUFFER_BIT);
                glColorMask(mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
                glClearColor(clear[0], clear[1], clear[2], clear[3]);
            }
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffers[next]);
            glBlitFramebuffer(0, 0, width, height, 0, height, width, 0, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            long frame = submit();
            pool.show(next, false);
            lastShown = next;
            finish(frame);
        } finally {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
            glBindTexture(0x84F5, rectangle);
            if (scissor) glEnable(GL_SCISSOR_TEST);
            if (srgb) glEnable(GL_FRAMEBUFFER_SRGB);
        }
    }

    private void measure() {
        long now = System.nanoTime();
        if (lastFrame != 0) {
            long frame = Math.max(0, now - lastFrame - idle);
            // exponential average over 8 frames, so slow frame rates react more slowly in time
            work = work == 0 ? frame : work + (frame - work) / 8;
            decide(now, frame);
        }
        lastFrame = now;
        idle = 0;
    }

    private void decide(long now, long frame) {
        if (work < OVERLAP_BELOW) {
            if (strict) switchTo(false);
            strictCount = overlapCount = 0;
            trial = false;
            retryDelay = FIRST_RETRY;
            return;
        }
        if (!strict && strictCount == 0 && work <= STRICT_ABOVE) return;
        if (++sinceSwitch > SETTLE) {
            if (strict) strictFrames[strictCount++ % strictFrames.length] = frame;
            else overlapFrames[overlapCount++ % overlapFrames.length] = frame;
        }
        if (sinceSwitch < TRIAL) return;
        if (trial) {
            trial = false;
            boolean wantStrict = strictCount == 0 || overlapCount == 0 ? strict
                    : median(strictFrames, strictCount) <= median(overlapFrames, overlapCount) * MAX_STRICT_COST;
            if (wantStrict != strict) switchTo(wantStrict);
            retryAt = now + retryDelay;
            retryDelay = Math.min(retryDelay * 2, LAST_RETRY);
            return;
        }
        if (now >= retryAt || (strict ? overlapCount : strictCount) == 0) {
            if (strict) overlapCount = 0;
            else strictCount = 0;
            switchTo(!strict);
            trial = true;
        }
    }

    private void switchTo(boolean strict) {
        this.strict = strict;
        sinceSwitch = 0;
    }

    private static long median(long[] frames, int count) {
        long[] sorted = Arrays.copyOf(frames, Math.min(count, frames.length));
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private void finish(long sync) {
        if (strict) awaitGpu(sync);
        else glDeleteSync(sync);
    }

    @Override
    public void idle(long nanos) {
        idle += nanos;
    }

    private static long submit() {
        long sync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        glFlush();
        return sync;
    }

    private static void awaitGpu(long sync) {
        if (sync == 0) throw new IllegalStateException("glFenceSync failed");
        int result = glClientWaitSync(sync, 0, 1_000_000_000L);
        glDeleteSync(sync);
        if (result == GL_WAIT_FAILED) {
            throw new IllegalStateException("glClientWaitSync failed: 0x" + Integer.toHexString(glGetError()));
        }
    }

    @Override
    public void pace() {
        checkContext();
        long start = System.nanoTime();
        Macos.pace(window);
        idle += System.nanoTime() - start;
    }

    @Override
    public void hide() {
        checkContext();
        pool.hide();
    }

    private void resize(int width, int height) {
        pool.hide();
        releaseSurfaces();
        this.width = width;
        this.height = height;
        try {
            pool.allocate(width, height);
            for (int i = 0; i < MacosSurfacePool.SIZE; i++) {
                textures[i] = glGenTextures();
                glBindTexture(0x84F5, textures[i]);
                int error = Macos.texture(context, pool.surfaces[i], width, height);
                if (error != 0) throw new IllegalStateException("CGLTexImageIOSurface2D failed: " + error);
                framebuffers[i] = glGenFramebuffers();
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffers[i]);
                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 0x84F5, textures[i], 0);
                requireFramebuffer(GL_DRAW_FRAMEBUFFER);
            }
        } catch (RuntimeException | Error failure) {
            releaseSurfaces();
            this.width = this.height = 0;
            throw failure;
        }
    }

    private static void requireFramebuffer(int target) {
        if (glCheckFramebufferStatus(target) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Presentation framebuffer incomplete");
        }
    }

    private void checkContext() {
        if (closed) throw new IllegalStateException("Presenter is closed");
        if (Thread.currentThread() != owner || Macos.currentContext() != context) {
            throw new IllegalStateException("Presentation requires the original OpenGL thread and context");
        }
    }

    private void releaseSurfaces() {
        if (attached >= 0) {
            int bound = glGetInteger(GL_FRAMEBUFFER_BINDING);
            detach();
            glBindFramebuffer(GL_FRAMEBUFFER, bound);
        }
        ownTextureStale = false;
        lastShown = -1;
        for (int i = 0; i < MacosSurfacePool.SIZE; i++) {
            if (framebuffers[i] != 0) glDeleteFramebuffers(framebuffers[i]);
            if (textures[i] != 0) glDeleteTextures(textures[i]);
            framebuffers[i] = textures[i] = 0;
        }
        pool.release();
    }

    @Override
    public void close() {
        if (closed) return;
        checkContext();
        Macos.stopDisplayLink();
        releaseSurfaces();
        pool.close();
        if (sourceFramebuffer != 0) glDeleteFramebuffers(sourceFramebuffer);
        closed = true;
    }
}
