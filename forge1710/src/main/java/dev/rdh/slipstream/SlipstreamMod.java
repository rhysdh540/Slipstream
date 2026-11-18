package dev.rdh.slipstream;

import cpw.mods.fml.common.Mod;

@Mod(modid = "slipstream", useMetadata = true, acceptableRemoteVersions = "*", dependencies = "required-after:lwjgl3ify@[3.0,)")
public class SlipstreamMod {
    public SlipstreamMod() {
        Presentation.setClientThread(Thread.currentThread());
    }
}
