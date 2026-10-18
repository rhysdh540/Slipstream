package dev.rdh.slipstream.b3d;

import dev.rdh.slipstream.GlPresenter;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
#if MC < 26
import org.lwjgl.glfw.GLFW;
#endif

public final class GlPresentation {
    private static final Logger LOG = LoggerFactory.getLogger("Slipstream");
    private static final boolean SDL = usesSdl();
    private static GlPresenter presenter;
    private static long window;
    private static boolean presented, failed;

    private GlPresentation() {}

    public static boolean usesSdl() {
        #if MC < 26
        return false;
        #else
        try {
        #if fabric
		return net.fabricmc.loader.api.FabricLoader.getInstance()
					.getModContainer("minecraft").get().getMetadata().getVersion()
                    .compareTo(net.fabricmc.loader.api.Version.parse("26.2")) > 0;
        #elif neoforge
            return org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec("(26.2,)")
                    .containsVersion(net.neoforged.fml.loading.FMLLoader.getCurrent().getLoadingModList()
                            .getModFileById("minecraft").getMods().getFirst().getVersion());
        #else
        #error "Unsupported loader"
        #endif
        } catch (Exception e) {
			throw new AssertionError(e);
		}
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
                LOG.info("Presenting without OpenGL swapping");
            }
            presenter.presentFramebuffer(framebuffer, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            presented = true;
            return true;
        } catch (RuntimeException | LinkageError failure) {
            failed = true;
            LOG.error("Presentation bypass disabled; falling back to OpenGL swapping", failure);
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
            LOG.error("Could not close the presenter", failure);
        } finally {
            presenter = null;
        }
    }
}
