package dev.rdh.slipstream.b3d.mixin;

#if MC >= 26
import dev.rdh.slipstream.VulkanPresenter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = {"com.mojang.blaze3d.vulkan.VulkanBackend", "com.mojang.renderpearl.backend.vulkan.VulkanBackend"})
abstract class VulkanBackendMixin {
    @Redirect(method = {
            "createDevice(Ljava/util/Collection;Lcom/mojang/blaze3d/vulkan/VulkanPhysicalDevice;Ljava/util/Set;)Lorg/lwjgl/vulkan/VkDevice;",
            "createDevice(Lcom/mojang/renderpearl/backend/vulkan/init/FeatureSet;Lcom/mojang/renderpearl/backend/vulkan/VulkanPhysicalDevice;)Lorg/lwjgl/vulkan/VkDevice;"
    }, at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkCreateDevice(Lorg/lwjgl/vulkan/VkPhysicalDevice;Lorg/lwjgl/vulkan/VkDeviceCreateInfo;Lorg/lwjgl/vulkan/VkAllocationCallbacks;Lorg/lwjgl/PointerBuffer;)I"))
    private static int slipstream$device(VkPhysicalDevice physical, VkDeviceCreateInfo info,
                                        VkAllocationCallbacks allocator, PointerBuffer result) {
        return VulkanPresenter.createDevice(physical, info, allocator, result);
    }
}
#endif
