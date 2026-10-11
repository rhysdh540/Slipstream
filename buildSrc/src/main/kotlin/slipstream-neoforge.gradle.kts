plugins {
    id("net.neoforged.moddev")
}

neoForge {
    version = prop("minecraft_version").removePrefix("1.") + "." + prop("forge_version")
}

apply(plugin = "slipstream-forge-common")

tasks.processResources {
    filesMatching("META-INF/mods.toml") {
        name = "neoforge.mods.toml"
    }
}
