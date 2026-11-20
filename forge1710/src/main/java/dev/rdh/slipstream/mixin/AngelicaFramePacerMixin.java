package dev.rdh.slipstream.mixin;

import dev.rdh.slipstream.Presentation;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.FramePacer", remap = false)
abstract class AngelicaFramePacerMixin {
    @WrapMethod(method = "endFrame", require = 0)
    private static long slipstream$limitFps(int capHz, Runnable renderAheadWait, Operation<Long> original) {
        long start = System.nanoTime();
        try {
            return original.call(capHz, renderAheadWait);
        } finally {
            Presentation.idle(System.nanoTime() - start);
        }
    }

    @WrapMethod(method = "beforePresent", require = 0)
    private static void slipstream$presentSleep(Operation<Void> original) {
        long start = System.nanoTime();
        try {
            original.call();
        } finally {
            Presentation.idle(System.nanoTime() - start);
        }
    }
}
