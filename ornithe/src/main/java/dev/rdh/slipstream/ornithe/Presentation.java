package dev.rdh.slipstream.ornithe;

import dev.rdh.slipstream.GlPresenter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.Display;
import org.lwjgl.sdl.SDLVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Presentation {
    private static final Logger LOG = LoggerFactory.getLogger("Slipstream");
    private static GlPresenter presenter;
    private static boolean failed, presented;

    private Presentation() {}

    public static boolean present(int framebuffer, boolean vsync) {
        return presented = show(framebuffer, vsync);
    }

    public static boolean presented() {
        return presented;
    }

    public static boolean consume() {
        boolean skip = presented;
        presented = false;
        return skip;
    }

    private static boolean show(int framebuffer, boolean vsync) {
        if (failed) return false;
        try {
            if (vsync || !Display.isVisible()) {
                if (presenter != null) presenter.hide();
                return false;
            }
            if (presenter == null) {
                presenter = create();
                if (presenter == null) {
                    failed = true;
                    LOG.info("No LWJGL 3 window found; install Pylon or legacy-lwjgl3 to bypass OpenGL swapping");
                    return false;
                }
                LOG.info("Presenting without OpenGL swapping");
            }
            presenter.presentFramebuffer(framebuffer, Display.getWidth(), Display.getHeight());
            return true;
        } catch (RuntimeException | LinkageError failure) {
            failed = true;
            LOG.error("Presentation bypass disabled; falling back to OpenGL swapping", failure);
            if (presenter != null) {
                try {
                    presenter.close();
                } catch (RuntimeException | LinkageError ignored) {
                }
                presenter = null;
            }
            return false;
        }
    }

    private static GlPresenter create() {
        long window = 0;
        try {
            window = SDLVideo.SDL_GL_GetCurrentWindow();
        } catch (LinkageError ignore) {
        }
        if (window != 0) return GlPresenter.create(window, true);
        try {
            window = GLFW.glfwGetCurrentContext();
        } catch (LinkageError ignore) {
        }
        return window != 0 ? GlPresenter.create(window, false) : null;
    }
}
