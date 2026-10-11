package dev.rdh.slipstream.macos;

import dev.rdh.slipstream.GlPresenter;

import static org.lwjgl.opengl.CGL.CGLGetCurrentContext;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public final class MacosPresenter implements GlPresenter {
    private final long context;
    private final MacosSurfacePool pool;
    private final Thread owner = Thread.currentThread();
    private final int[] textures = new int[MacosSurfacePool.SIZE];
    private final int[] framebuffers = new int[MacosSurfacePool.SIZE];
    private int sourceFramebuffer;
    private int width, height;
    private boolean closed;

    public MacosPresenter(long window) {
        if (window == 0) throw new IllegalArgumentException("Missing Cocoa window");
        context = CGLGetCurrentContext();
        if (context == 0) throw new IllegalStateException("No current OpenGL context");
        pool = new MacosSurfacePool(window);
    }

    @Override
    public void present(int texture, int width, int height) {
        if (texture == 0) throw new IllegalArgumentException("Invalid texture");
        present(texture, 0, width, height);
    }

    @Override
    public void presentFramebuffer(int framebuffer, int width, int height) {
        if (framebuffer < 0) throw new IllegalArgumentException("Invalid framebuffer");
        present(0, framebuffer, width, height);
    }

    private void present(int texture, int framebuffer, int width, int height) {
        checkContext();
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid frame");
        int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int rectangle = glGetInteger(0x84F6);
        boolean scissor = glIsEnabled(GL_SCISSOR_TEST);
        boolean srgb = glIsEnabled(GL_FRAMEBUFFER_SRGB);
        try {
            if (this.width != width || this.height != height) resize(width, height);
            int next = pool.free(i -> true);
            if (next < 0) {
                glFlush();
                return;
            }
            if (texture != 0) {
                if (sourceFramebuffer == 0) sourceFramebuffer = glGenFramebuffers();
                framebuffer = sourceFramebuffer;
            }
            glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
            if (texture != 0) glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
            requireFramebuffer(GL_READ_FRAMEBUFFER);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffers[next]);
            glDisable(GL_SCISSOR_TEST);
            glDisable(GL_FRAMEBUFFER_SRGB);
            glBlitFramebuffer(0, 0, width, height, 0, height, width, 0, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            glFlush();
            pool.show(next);
        } finally {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
            glBindTexture(0x84F5, rectangle);
            if (scissor) glEnable(GL_SCISSOR_TEST);
            if (srgb) glEnable(GL_FRAMEBUFFER_SRGB);
        }
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
        if (Thread.currentThread() != owner || CGLGetCurrentContext() != context) {
            throw new IllegalStateException("Presentation requires the original OpenGL thread and context");
        }
    }

    private void releaseSurfaces() {
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
        releaseSurfaces();
        pool.close();
        if (sourceFramebuffer != 0) glDeleteFramebuffers(sourceFramebuffer);
        closed = true;
    }
}
