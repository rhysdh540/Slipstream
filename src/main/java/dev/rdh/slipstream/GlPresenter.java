package dev.rdh.slipstream;

import org.lwjgl.system.Platform;

import dev.rdh.slipstream.macos.Macos;
import dev.rdh.slipstream.macos.MacosPresenter;

public interface GlPresenter extends AutoCloseable {
    static GlPresenter create(long window, boolean sdl) {
		if(Platform.get() == Platform.MACOSX) {
			return new MacosPresenter(Macos.cocoaWindow(window, sdl));
		}
	    return null;
	}

    void present(int texture, int width, int height);

    void presentFramebuffer(int framebuffer, int width, int height);

    /**
     * Like {@link #presentFramebuffer(int, int, int)}, but with {@code direct} the presenter may make the framebuffer
     * render straight into presentation surfaces from the next frame on (by swapping its colour attachment), so frames
     * are shown without a copy. Pass false whenever something else needs the framebuffer's own colour texture (open
     * screens, post-processing); the presenter then gives it back before presenting by copy again.
     */
    default void presentFramebuffer(int framebuffer, int width, int height, boolean direct) {
        presentFramebuffer(framebuffer, width, height);
    }

    /** If direct presentation left the framebuffer's own colour texture behind, copy the newest frame into it. */
    default void syncFramebuffer() {}

    void pace();

    void idle(long nanos);

    void hide();

    @Override void close();
}
