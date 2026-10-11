package dev.rdh.slipstream.b3d;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.util.List;
#if forgelike
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Set;
#endif

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

	#if forgelike
	@Override public void onLoad(String mixinPackage) {}
	@Override public String getRefMapperConfig() { return null; }
	@Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
	@Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
	@Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
	@Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
	#endif
}
