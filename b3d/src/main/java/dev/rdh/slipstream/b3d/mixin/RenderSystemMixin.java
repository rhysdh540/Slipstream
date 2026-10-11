package dev.rdh.slipstream.b3d.mixin;

#if MC < 26
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.rdh.slipstream.b3d.GlPresentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSystem.class)
abstract class RenderSystemMixin {
    @WrapWithCondition(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V", remap = false))
    private static boolean slipstream$swap(long window) {
        return !GlPresentation.consume(window);
    }
}
#endif
