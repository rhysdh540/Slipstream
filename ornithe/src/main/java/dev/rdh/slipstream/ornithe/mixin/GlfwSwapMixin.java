package dev.rdh.slipstream.ornithe.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.slipstream.ornithe.Presentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "org.lwjgl.opengl.GLFWDisplay", remap = false)
abstract class GlfwSwapMixin {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"))
    private void slipstream$swap(long window, Operation<Void> original) {
        if (!Presentation.consume()) original.call(window);
    }
}
