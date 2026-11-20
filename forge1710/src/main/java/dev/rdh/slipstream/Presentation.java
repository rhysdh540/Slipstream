package dev.rdh.slipstream;

import dev.rdh.slipstream.macos.Macos;
import me.eigenraven.lwjgl3ify.client.MainThreadExec;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjglx.opengl.Display;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public final class Presentation {
    private static final Logger LOG = LogManager.getLogger("Slipstream");
    // forge's loading screen uses its own thread, we don't want that
    private static Thread client;
    private static GlPresenter presenter;
    private static boolean failed, presented, vsync, unswapped;
    private static final MethodHandle SWAPS = swaps();

    private Presentation() {}

    public static void setClientThread(Thread thread) {
        client = thread;
    }

    public static boolean present(int framebuffer, int width, int height) {
        if (Thread.currentThread() != client) return false;
        if (unswapped) {
            disable(new IllegalStateException("Minecraft's Display.update call is not hooked; another mod may have replaced it"));
            return false;
        }
        presented = show(framebuffer, width, height);
        unswapped = presented;
        return presented;
    }

    private static MethodHandle swaps() {
        try {
            return MethodHandles.privateLookupIn(Display.class, MethodHandles.lookup())
                    .findStaticSetter(Display.class, "hasGLContext", boolean.class);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            LOG.error("Cannot skip lwjgl3ify's swap; presenting through it", failure);
            return null;
        }
    }

    public static void update(Runnable update) {
        unswapped = false;
        presentWindow();
        if (!consume() || SWAPS == null) {
            update.run();
            return;
        }
        try {
            SWAPS.invokeExact(false);
            try {
                update.run();
            } finally {
                SWAPS.invokeExact(true);
            }
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void presentWindow() {
        if (presented || Thread.currentThread() != client) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        presented = show(0, minecraft.displayWidth, minecraft.displayHeight);
    }

    private static boolean consume() {
        if (Thread.currentThread() != client) return false;
        boolean skip = presented;
        presented = false;
        if (skip && vsync) {
            try {
                presenter.pace();
            } catch (RuntimeException | LinkageError failure) {
                // this frame is already on screen, so the swap is still skipped; the next one swaps
                disable(failure);
            }
        }
        return skip;
    }

    public static void idle(long nanos) {
        if (presenter != null && Thread.currentThread() == client) presenter.idle(nanos);
    }

    private static boolean show(int framebuffer, int width, int height) {
        if (failed) return false;
        try {
            vsync = Minecraft.getMinecraft().gameSettings.enableVsync;
            if (!Display.isVisible()) {
                if (presenter != null) presenter.hide();
                return false;
            }
            if (presenter == null) {
                // the client thread isn't the main thread here, and RetroFuturaBootstrap's loop owns the main thread
                Macos.setMainThread(MainThreadExec::runOnMainThread);
                presenter = GlPresenter.create(Display.getWindow(), true);
                if (presenter == null) {
                    failed = true;
                    return false;
                }
                LOG.info("Presenting without OpenGL swapping");
            }
            presenter.presentFramebuffer(framebuffer, width, height);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            disable(failure);
            return false;
        }
    }

    private static void disable(Throwable failure) {
        failed = true;
        LOG.error("Presentation bypass disabled; falling back to OpenGL swapping", failure);
        if (presenter != null) {
            try {
                presenter.close();
            } catch (RuntimeException | LinkageError ignored) {
            }
            presenter = null;
        }
    }
}
