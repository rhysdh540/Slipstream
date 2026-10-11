package dev.rdh.slipstream.b3d.mixin;

#if MC >= 26
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import dev.rdh.slipstream.b3d.GlPresentation;

@Pseudo
@Mixin(targets = {"com.mojang.blaze3d.opengl.GlSurface", "com.mojang.renderpearl.backend.opengl.GlSurface"})
abstract class GlSurfaceMixin {
    @WrapWithCondition(method = "present", at = {
            @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"),
            @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SwapWindow(J)Z")
    })
    private boolean slipstream$swap(long window) {
        return !GlPresentation.consume(window);
    }
}
#endif
