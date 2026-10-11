package dev.rdh.slipstream.b3d.mixin;

#if MC >= 26
import dev.rdh.slipstream.b3d.VulkanPresentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = {"com.mojang.blaze3d.vulkan.VulkanCommandEncoder", "com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder"})
abstract class VulkanCommandEncoderMixin implements VulkanPresentation.Encoder {
    @Shadow public abstract void signalSemaphore(long semaphore, long value, long stage);
}
#endif
