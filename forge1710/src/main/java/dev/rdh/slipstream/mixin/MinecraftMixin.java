package dev.rdh.slipstream.mixin;

import dev.rdh.slipstream.Presentation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// we can't mixin to lwjgl because lwjgl3ify blocks that, so wrap the calls here instead
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @WrapOperation(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V"), require = 0)
    private void slipstream$present(Framebuffer framebuffer, int width, int height, Operation<Void> original) {
        if (framebuffer != Minecraft.getMinecraft().getFramebuffer() || framebuffer.framebufferObject < 0
                || !Presentation.present(framebuffer.framebufferObject, width, height)) {
            original.call(framebuffer, width, height);
        }
    }

    @WrapOperation(method = "func_147120_f", at = {
            @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;update()V", remap = false),
            @At(value = "INVOKE", target = "Lcom/gtnewhorizons/angelica/glsm/GLStateManager;updateDisplay()V", remap = false)
    }, require = 0)
    private void slipstream$update(Operation<Void> original) {
        Presentation.update(original::call);
    }

    @WrapOperation(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;sync(I)V", remap = false), require = 0)
    private void slipstream$limitFps(int fps, Operation<Void> original) {
        long start = System.nanoTime();
        original.call(fps);
        Presentation.idle(System.nanoTime() - start);
    }
}
