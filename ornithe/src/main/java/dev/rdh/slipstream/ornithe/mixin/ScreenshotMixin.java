package dev.rdh.slipstream.ornithe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rdh.slipstream.ornithe.Presentation;
import net.minecraft.client.render.pipeline.RenderTarget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

import java.io.File;

/**
 * Screenshots read the main framebuffer's colour texture, which direct presentation can leave a frame behind; copy
 * the newest frame into it first. A high-priority wrap so it also runs when another mod replaces the method body.
 */
@Mixin(targets = "net.minecraft.client.Screenshot", priority = 2000)
abstract class ScreenshotMixin {
    @WrapMethod(method = "take(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/render/pipeline/RenderTarget;)Lnet/minecraft/text/Text;", require = 0)
    private static Text slipstream$syncBeforeScreenshot(File dir, String name, int width, int height, RenderTarget target, Operation<Text> original) {
        Presentation.syncFramebuffer();
        return original.call(dir, name, width, height, target);
    }
}
