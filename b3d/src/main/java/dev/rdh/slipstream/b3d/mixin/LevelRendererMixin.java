package dev.rdh.slipstream.b3d.mixin;

#if MC >= 21.05
import net.minecraft.client.renderer.LevelRenderer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.lwjgl.system.Platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @ModifyExpressionValue(
            #if MC >= 26 || neoforge
            method = {"lambda$render$0", "lambda$renderLevel$0"},
            #else
            method = "method_62218",
            #endif
            at = @At(value = "CONSTANT", args = "floatValue=0.0F")
    )
    private float slipstream$opaqueClear(float alpha) {
        return Platform.get() == Platform.MACOSX ? 1.0F : alpha;
    }
}
#endif
