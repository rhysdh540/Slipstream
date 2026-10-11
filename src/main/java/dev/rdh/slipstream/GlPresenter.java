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

    void hide();

    @Override void close();
}
