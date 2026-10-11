package dev.rdh.slipstream.b3d.mixin;

#if MC >= 21.05
import dev.rdh.slipstream.b3d.GlPresentation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
#if MC >= 26
@Mixin(targets = {"com.mojang.blaze3d.opengl.GlCommandEncoder", "com.mojang.renderpearl.backend.opengl.GlCommandEncoder"})
#else
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
#endif
abstract class GlCommandEncoderMixin {
    @Shadow @Final private int drawFbo;

    @Inject(method = "presentTexture", at = {
            @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/DirectStateAccess;blitFrameBuffers(IIIIIIIIIIII)V"),
            #if MC >= 26
            @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/opengl/DirectStateAccess;blitFrameBuffers(IIIIIIIIIIII)V")
            #endif
    }, cancellable = true)
    private void slipstream$present(CallbackInfo ci) {
        if (GlPresentation.present(drawFbo)) ci.cancel();
    }

    #if MC >= 26
    @Inject(method = "close", at = @At("HEAD"), require = 0)
    private void slipstream$close(CallbackInfo ci) {
        GlPresentation.close();
    }
    #endif
}
#endif
