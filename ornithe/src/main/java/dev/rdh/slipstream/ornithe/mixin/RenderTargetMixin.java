package dev.rdh.slipstream.ornithe.mixin;

import dev.rdh.slipstream.ornithe.Presentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.client.render.pipeline.RenderTarget")
abstract class RenderTargetMixin {
    @Shadow public int frameBufferId;
    @Shadow public abstract void unbindRead();

    @Inject(method = "draw(IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/pipeline/RenderTarget;bindRead()V"), cancellable = true, require = 0)
    private void slipstream$skipQuad(int width, int height, boolean disableBlend, CallbackInfo ci) {
        if (!slipstream$present()) return;
        unbindRead();
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);
        ci.cancel();
    }

    // for older versions without glsm
    @Inject(method = "draw(II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/pipeline/RenderTarget;bindRead()V"), cancellable = true, require = 0)
    private void slipstream$skipLegacyQuad(int width, int height, CallbackInfo ci) {
        if (!slipstream$present()) return;
        unbindRead();
        GL11.glDepthMask(true);
        GL11.glColorMask(true, true, true, true);
        ci.cancel();
    }

    private boolean slipstream$present() {
        Minecraft client = Minecraft.getInstance();
        return (Object) this == client.getRenderTarget() && frameBufferId >= 0
                && Presentation.present(frameBufferId, client.options.vsync);
    }
}
