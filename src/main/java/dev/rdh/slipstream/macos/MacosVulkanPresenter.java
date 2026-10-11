package dev.rdh.slipstream.macos;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import dev.rdh.slipstream.VulkanPresenter;

import java.util.function.IntPredicate;

import static org.lwjgl.vulkan.VK12.*;

public final class MacosVulkanPresenter implements VulkanPresenter {
    private final VkDevice device;
    private final Thread owner = Thread.currentThread();
    private final long[] images = new long[MacosSurfacePool.SIZE], memory = new long[MacosSurfacePool.SIZE], values = new long[MacosSurfacePool.SIZE];
    private MacosSurfacePool pool;
    private long semaphore, sequence, displayed, completed;
    private final IntPredicate finished = i -> values[i] <= completed;
    private int selected = -1;
    private boolean closed;

    public MacosVulkanPresenter(VkDevice device, long window, int width, int height, int format) {
        this.device = device;
        if (window == 0 || width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid presentation window");
        if (!device.getCapabilities().VK_EXT_metal_objects) throw new UnsupportedOperationException("VK_EXT_metal_objects unavailable");
        if (format != VK_FORMAT_B8G8R8A8_UNORM && format != VK_FORMAT_B8G8R8A8_SRGB) {
            throw new UnsupportedOperationException("Unsupported IOSurface format: " + format);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            pool = new MacosSurfacePool(window);
            pool.allocate(width, height);
            var result = stack.callocLong(1);
            var type = VkSemaphoreTypeCreateInfo.calloc(stack).sType$Default().semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE);
            check(vkCreateSemaphore(device, VkSemaphoreCreateInfo.calloc(stack).sType$Default().pNext(type), null, result));
            semaphore = result.get(0);
            var properties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), properties);
            for (int i = 0; i < images.length; i++) {
                var imported = VkImportMetalIOSurfaceInfoEXT.calloc(stack).sType$Default().ioSurface(pool.surfaces[i]);
                var info = VkImageCreateInfo.calloc(stack).sType$Default().pNext(imported)
                        .imageType(VK_IMAGE_TYPE_2D).format(format).mipLevels(1).arrayLayers(1)
                        .samples(VK_SAMPLE_COUNT_1_BIT).tiling(VK_IMAGE_TILING_OPTIMAL)
                        .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
                info.extent().set(width, height, 1);
                check(vkCreateImage(device, info, null, result));
                images[i] = result.get(0);
                var requirements = VkMemoryRequirements.calloc(stack);
                vkGetImageMemoryRequirements(device, images[i], requirements);
                int memoryType = -1;
                for (int j = 0; j < properties.memoryTypeCount(); j++) {
                    if ((requirements.memoryTypeBits() & (1 << j)) != 0) {
                        memoryType = j;
                        if ((properties.memoryTypes(j).propertyFlags() & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) break;
                    }
                }
                if (memoryType < 0) throw new IllegalStateException("No compatible IOSurface memory type");
                var dedicated = VkMemoryDedicatedAllocateInfo.calloc(stack).sType$Default().image(images[i]);
                check(vkAllocateMemory(device, VkMemoryAllocateInfo.calloc(stack).sType$Default().pNext(dedicated)
                        .allocationSize(requirements.size()).memoryTypeIndex(memoryType), null, result));
                memory[i] = result.get(0);
                check(vkBindImageMemory(device, images[i], memory[i], 0));
            }
        } catch (RuntimeException | Error failure) {
            release();
            throw failure;
        }
    }

    @Override
    public boolean acquire() {
        checkThread();
        present();
        selected = pool.free(finished);
        return selected >= 0;
    }

    @Override
    public long image() {
        checkThread();
        if (selected < 0) throw new IllegalStateException("No acquired IOSurface image");
        return images[selected];
    }

    @Override public long semaphore() { return semaphore; }

    @Override
    public long submitted() {
        checkThread();
        if (selected < 0) throw new IllegalStateException("No acquired IOSurface image");
        values[selected] = ++sequence;
        selected = -1;
        return sequence;
    }

    @Override
    public void present() {
        checkThread();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var value = stack.callocLong(1);
            check(vkGetSemaphoreCounterValue(device, semaphore, value));
            completed = value.get(0);
        }
        int newest = -1;
        for (int i = 0; i < images.length; i++) {
            if (values[i] > displayed && values[i] <= completed && (newest < 0 || values[i] > values[newest])) newest = i;
        }
        if (newest < 0) return;
        pool.show(newest);
        displayed = values[newest];
    }

    private void checkThread() {
        if (closed || Thread.currentThread() != owner) throw new IllegalStateException("Presentation requires its original Vulkan thread");
    }

    private static void check(int result) {
        if (result != VK_SUCCESS) throw new IllegalStateException("Vulkan IOSurface operation failed: " + result);
    }

    private void release() {
        for (int i = 0; i < images.length; i++) {
            if (images[i] != 0) vkDestroyImage(device, images[i], null);
            if (memory[i] != 0) vkFreeMemory(device, memory[i], null);
        }
        if (pool != null) pool.close();
        if (semaphore != 0) vkDestroySemaphore(device, semaphore, null);
    }

    @Override public void close() {
        if (closed) return;
        checkThread();
        check(vkDeviceWaitIdle(device));
        release();
        closed = true;
    }
}
