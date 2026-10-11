package dev.rdh.slipstream.b3d;

#if MC >= 26
import dev.rdh.slipstream.VulkanPresenter;
import net.minecraft.client.Minecraft;
import org.lwjgl.vulkan.VkDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VulkanPresentation {
    private static final Logger LOG = LoggerFactory.getLogger("Slipstream");

    private VulkanPresentation() {}

    public interface Encoder {
        void signalSemaphore(long semaphore, long value, long stage);
    }

    public static VulkanPresenter create(Object surface, int width, int height, int format) {
        try {
            var field = surface.getClass().getDeclaredField("device");
            field.setAccessible(true);
            Object backend = field.get(surface);
            VkDevice device = (VkDevice) backend.getClass().getMethod("vkDevice").invoke(backend);
            var window = Minecraft.getInstance().getWindow();
            var presenter = VulkanPresenter.create(device, window.handle(), GlPresentation.usesSdl(), width, height, format);
            if (presenter != null) LOG.info("Presenting Vulkan without the swapchain; native acquisition and presentation bypassed");
            return presenter;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            LOG.error("Vulkan presenter initialization failed; retaining native swapchain", failure);
            return null;
        }
    }
}
#endif
