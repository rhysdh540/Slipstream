package dev.rdh.slipstream;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.*;

import dev.rdh.slipstream.macos.Macos;
import dev.rdh.slipstream.macos.MacosVulkanPresenter;

import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTMetalObjects.VK_EXT_METAL_OBJECTS_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK12.*;

public interface VulkanPresenter extends AutoCloseable {
    private static String extension() {
        return Platform.get() == Platform.MACOSX ? VK_EXT_METAL_OBJECTS_EXTENSION_NAME : null;
    }

    static int createDevice(VkPhysicalDevice physical, VkDeviceCreateInfo info,
                            VkAllocationCallbacks allocator, PointerBuffer result) {
        String extension = extension();
        if (extension == null) return vkCreateDevice(physical, info, allocator, result);
        // No stackPush: the caller reads the extension names back out of info after this returns.
        MemoryStack stack = MemoryStack.stackGet();
        var count = stack.callocInt(1);
        if (vkEnumerateDeviceExtensionProperties(physical, (String) null, count, null) != VK_SUCCESS) {
            return vkCreateDevice(physical, info, allocator, result);
        }
        var extensions = VkExtensionProperties.calloc(count.get(0), stack);
        if (vkEnumerateDeviceExtensionProperties(physical, (String) null, count, extensions) != VK_SUCCESS) {
            return vkCreateDevice(physical, info, allocator, result);
        }
        boolean supported = false;
        for (var available : extensions) supported |= extension.equals(available.extensionNameString());
        PointerBuffer original = info.ppEnabledExtensionNames();
        int size = original == null ? 0 : original.remaining();
        for (int i = 0; i < size; i++) supported &= !extension.equals(memUTF8(original.get(original.position() + i)));
        if (supported) {
            var names = stack.mallocPointer(size + 1);
            for (int i = 0; i < size; i++) names.put(original.get(original.position() + i));
            names.put(stack.UTF8(extension)).flip();
            info.ppEnabledExtensionNames(names);
        }
        return vkCreateDevice(physical, info, allocator, result);
    }

    static VulkanPresenter create(VkDevice device, long window, boolean sdl, int width, int height, int format) {
		if(Platform.get() == Platform.MACOSX) {
			return new MacosVulkanPresenter(device, Macos.cocoaWindow(window, sdl), width, height, format);
		}
	    return null;
	}

    boolean acquire();

    long image();

    long semaphore();

    long submitted();

    void present();

    @Override void close();
}
