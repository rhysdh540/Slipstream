package dev.rdh.slipstream.b3d.mixin;

#if MC >= 26
import dev.rdh.slipstream.b3d.GlPresentation;
import net.minecraft.client.FramerateLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FramerateLimiter.class)
abstract class FramerateLimiterMixin {
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
