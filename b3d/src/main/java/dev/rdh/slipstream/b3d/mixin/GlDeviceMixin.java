package dev.rdh.slipstream.b3d.mixin;

#if MC >= 26
import dev.rdh.slipstream.b3d.GlPresentation;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
abstract class GlDeviceMixin {
    @WrapWithCondition(method = "presentFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), require = 0)
    private boolean slipstream$swap(long window) {
        return !GlPresentation.consume(window);
    }
}
#endif
