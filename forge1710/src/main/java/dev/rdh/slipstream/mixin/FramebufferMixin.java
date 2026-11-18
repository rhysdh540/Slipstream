package dev.rdh.slipstream.mixin;

import dev.rdh.slipstream.Presentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Framebuffer.class)
abstract class FramebufferMixin {
    @Shadow public int framebufferObject;

    // after the state setup and before the full-screen quad, so everything but the draw is as vanilla leaves it
    @Inject(method = "framebufferRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;bindFramebufferTexture()V"), cancellable = true)
    private void slipstream$present(int width, int height, CallbackInfo ci) {
        if ((Object) this != Minecraft.getMinecraft().getFramebuffer() || framebufferObject < 0) return;
        if (!Presentation.present(framebufferObject, width, height)) return;
        GL11.glDepthMask(true);
        GL11.glColorMask(true, true, true, true);
        ci.cancel();
    }
}
