package dev.rdh.slipstream.b3d.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.rdh.slipstream.b3d.GlPresentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
abstract class WindowMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void slipstream$close(CallbackInfo ci) {
        GlPresentation.close();
    }

    @Inject(method = "updateVsync", at = @At("HEAD"), require = 0)
    private void slipstream$vsync(boolean enabled, CallbackInfo ci) {
        if (enabled) GlPresentation.hide();
    }
}
