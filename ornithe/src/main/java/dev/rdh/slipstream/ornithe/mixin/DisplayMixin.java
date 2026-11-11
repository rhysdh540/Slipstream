package dev.rdh.slipstream.ornithe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.rdh.slipstream.ornithe.Presentation;

import net.minecraft.client.Minecraft;

@Mixin(value = Display.class, remap = false)
public class DisplayMixin {
	@Inject(method = "update", at = @At("HEAD"))
	private static void slipstream$update(CallbackInfo ci) {
		if (!Presentation.presented()) {
			boolean vsync;
			Minecraft mc = Minecraft.getInstance();
			if (mc != null && mc.options != null) {
				vsync = mc.options.vsync;
			} else {
				vsync = false;
			}
			Presentation.present(0, vsync);
		}
	}

	@WrapMethod(method = "sync")
	private static void slipstream$limit(int fps, Operation<Void> original) {
		long start = System.nanoTime();
		original.call(fps);
		Presentation.idle(System.nanoTime() - start);
	}
}
