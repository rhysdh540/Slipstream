package dev.rdh.slipstream.b3d.mixin;

#if MC < 21.05
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.rdh.slipstream.b3d.GlPresentation;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @WrapWithCondition(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V"))
    private boolean slipstream$present(RenderTarget target, int width, int height) {
        return !GlPresentation.present(target.frameBufferId);
    }
}
#endif
