package dev.rdh.slipstream.ornithe.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.slipstream.ornithe.Presentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = {"pl.tomgirl.pylon.window.GlSurface", "org.lwjgl.opengl.SDLDisplay"}, remap = false)
abstract class SdlSwapMixin {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SwapWindow(J)Z"))
    private boolean slipstream$swap(long window, Operation<Boolean> original) {
        return Presentation.consume() || original.call(window);
    }
}
