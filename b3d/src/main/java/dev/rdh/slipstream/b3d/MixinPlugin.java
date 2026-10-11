package dev.rdh.slipstream.b3d;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.util.List;

public class MixinPlugin implements IMixinConfigPlugin {
	@Override
	public List<String> getMixins() {
		#if MC >= 26
		return List.of("GlCommandEncoderMixin", "GlDeviceMixin", "GlSurfaceMixin", "LevelRendererMixin",
				"VulkanBackendMixin", "VulkanCommandEncoderMixin", "VulkanSurfaceMixin");
		#elif MC >= 21.05
		return List.of("GlCommandEncoderMixin", "LevelRendererMixin", "RenderSystemMixin");
		#else
		return List.of("MinecraftMixin", "RenderSystemMixin");
		#endif
	}
}
