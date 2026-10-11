plugins {
    id("java")
}

apply(plugin = if (unobfuscated) "net.fabricmc.fabric-loom" else "net.fabricmc.fabric-loom-remap")

dependencies {
    "minecraft"("com.mojang:minecraft:${propMaybe("minecraftVersion")?.takeIf { unobfuscated } ?: prop("minecraft_version")}")
    if (unobfuscated) {
        implementation("net.fabricmc:fabric-loader:0.19.5")
    } else {
        "mappings"(loom.officialMojangMappings())
        "modImplementation"("net.fabricmc:fabric-loader:0.19.5")
    }
}

tasks.processResources {
    exclude("META-INF/mods.toml")
}
