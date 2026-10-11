package dev.rdh.slipstream.b3d.mixin;

#if MC >= 26
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.rdh.slipstream.VulkanPresenter;
import dev.rdh.slipstream.b3d.VulkanPresentation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

@Pseudo
@Mixin(targets = {"com.mojang.blaze3d.vulkan.VulkanGpuSurface", "com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface"})
abstract class VulkanSurfaceMixin {
    @Shadow private int currentImageIndex;
    @Shadow private int swapchainWidth;
    @Shadow private int swapchainHeight;
    @Shadow @Final private int swapchainImageFormat;
    @Unique private VulkanPresenter slipstream$presenter;
    @Unique private boolean slipstream$failed;

    @Inject(method = "acquireNextTexture", at = @At("HEAD"), cancellable = true)
    private void slipstream$acquire(CallbackInfo ci) {
        if (slipstream$failed) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.options.enableVsync().get() || minecraft.getWindow().isIconified()) {
            if (slipstream$presenter != null) slipstream$close(ci);
            return;
        }
        if (slipstream$presenter == null) {
            slipstream$presenter = VulkanPresentation.create(this, swapchainWidth, swapchainHeight, swapchainImageFormat);
            slipstream$failed = slipstream$presenter == null;
        }
        if (slipstream$presenter != null) {
            currentImageIndex = 0;
            ci.cancel();
        }
    }

    @Inject(method = "blitFromTexture", at = @At("HEAD"), cancellable = true)
    private void slipstream$blit(CallbackInfo ci) {
        if (slipstream$presenter != null && !slipstream$presenter.acquire()) ci.cancel();
    }

    @WrapOperation(method = "blitFromTexture", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongList;getLong(I)J"))
    private long slipstream$image(LongList images, int index, Operation<Long> original) {
        return slipstream$presenter == null ? original.call(images, index) : slipstream$presenter.image();
    }

    @ModifyArg(method = "blitFromTexture", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkImageMemoryBarrier2$Buffer;newLayout(I)Lorg/lwjgl/vulkan/VkImageMemoryBarrier2$Buffer;"))
    private int slipstream$layout(int layout) {
        return slipstream$presenter != null && layout == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR ? VK_IMAGE_LAYOUT_GENERAL : layout;
    }

    @WrapWithCondition(method = "blitFromTexture", at = {
            @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vulkan/VulkanCommandEncoder;waitSemaphore(JJJ)V"),
            @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;waitSemaphore(JJJ)V"),
            @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vulkan/VulkanCommandEncoder;signalSemaphore(JJJ)V"),
            @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;signalSemaphore(JJJ)V")
    })
    private boolean slipstream$nativeSemaphore(@Coerce Object encoder, long semaphore, long value, long stage) {
        return slipstream$presenter == null;
    }

    @Inject(method = "blitFromTexture", at = @At("TAIL"))
    private void slipstream$signal(@Coerce Object encoder, @Coerce Object texture, CallbackInfo ci) {
        if (slipstream$presenter != null) {
            ((VulkanPresentation.Encoder) encoder).signalSemaphore(slipstream$presenter.semaphore(),
                    slipstream$presenter.submitted(), VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
        }
    }

    @Inject(method = "present", at = @At("HEAD"), cancellable = true)
    private void slipstream$present(CallbackInfo ci) {
        if (slipstream$presenter != null) {
            slipstream$presenter.present();
            currentImageIndex = -1;
            ci.cancel();
        }
    }

    @Inject(method = "destroySwapchain", at = @At("HEAD"))
    private void slipstream$close(CallbackInfo ci) {
        if (slipstream$presenter != null) {
            slipstream$presenter.close();
            slipstream$presenter = null;
        }
        currentImageIndex = -1;
    }
}
#endif
