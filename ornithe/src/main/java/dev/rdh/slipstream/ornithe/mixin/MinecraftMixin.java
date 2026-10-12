package dev.rdh.slipstream.ornithe.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.rdh.slipstream.ornithe.Presentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Shadow public GameOptions options;

    @WrapWithCondition(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/pipeline/RenderTarget;draw(II)V"), require = 0)
    private boolean slipstream$present(RenderTarget target, int width, int height) {
        return target.frameBufferId < 0 || !Presentation.present(target.frameBufferId, options.vsync);
    }

    @Inject(method = "*", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;update()V", remap = false))
    private void slipstream$update(CallbackInfo ci) {
        if (!Presentation.presented()) Presentation.present(0, options.vsync);
    }
}
