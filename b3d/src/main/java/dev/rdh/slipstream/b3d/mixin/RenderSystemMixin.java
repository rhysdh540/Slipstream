package dev.rdh.slipstream.b3d.mixin;

#if MC < 26
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.rdh.slipstream.b3d.GlPresentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
abstract class RenderSystemMixin {
    @WrapWithCondition(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V", remap = false))
    private static boolean slipstream$swap(long window) {
        return !GlPresentation.consume(window);
    }

    @Inject(method = "limitDisplayFPS", at = @At("HEAD"), require = 0)
    private static void slipstream$startLimit(int fps, CallbackInfo ci) {
        GlPresentation.startLimit();
    }

    @Inject(method = "limitDisplayFPS", at = @At("RETURN"), require = 0)
    private static void slipstream$endLimit(int fps, CallbackInfo ci) {
        GlPresentation.endLimit();
    }
}
#endif
