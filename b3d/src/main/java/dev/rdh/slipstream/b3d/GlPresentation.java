package dev.rdh.slipstream.b3d;

import dev.rdh.slipstream.GlPresenter;
import net.minecraft.client.Minecraft;
#if MC >= 26
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
#else
import org.lwjgl.glfw.GLFW;
#endif

public final class GlPresentation {
    private static final System.Logger LOG = System.getLogger("Slipstream");
    private static final boolean SDL = usesSdl();
    private static GlPresenter presenter;
    private static long window;
    private static boolean presented, failed;

    private GlPresentation() {}

    public static boolean usesSdl() {
        #if MC >= 26
        try {
            return FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata()
                    .getVersion().compareTo(Version.parse("26.3-alpha.0")) >= 0;
        } catch (VersionParsingException failure) {
            throw new IllegalStateException(failure);
        }
        #else
        return false;
        #endif
    }

    public static boolean present(int framebuffer) {
        presented = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (failed) return false;
        try {
            #if MC >= 26
            long handle = minecraft.getWindow().handle();
            boolean iconified = minecraft.getWindow().isIconified();
            #else
            long handle = GLFW.glfwGetCurrentContext();
            boolean iconified = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) != 0;
            #endif
            #if MC >= 19
            boolean vsync = minecraft.options.enableVsync().get();
            #else
            boolean vsync = minecraft.options.enableVsync;
            #endif
            if (vsync || iconified) {
                hide();
                return false;
            }
            if (presenter == null) {
                window = handle;
                presenter = GlPresenter.create(window, SDL);
                if (presenter == null) {
                    failed = true;
                    return false;
                }
                LOG.log(System.Logger.Level.INFO, "Presenting without OpenGL swapping");
            }
            presenter.presentFramebuffer(framebuffer, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            presented = true;
            return true;
        } catch (RuntimeException | LinkageError failure) {
            failed = true;
            LOG.log(System.Logger.Level.ERROR, "Presentation bypass disabled; falling back to OpenGL swapping", failure);
            close();
            return false;
        }
    }

    public static boolean consume(long handle) {
        boolean skip = presented && window == handle;
        presented = false;
        return skip;
    }

    public static void hide() {
        presented = false;
        if (presenter != null) presenter.hide();
    }

    public static void close() {
        presented = false;
        if (presenter == null) return;
        try {
            presenter.close();
        } catch (RuntimeException | LinkageError failure) {
            LOG.log(System.Logger.Level.ERROR, "Could not close the presenter", failure);
        } finally {
            presenter = null;
        }
    }
}
