import net.neoforged.moddevgradle.legacyforge.tasks.RemapJar

plugins {
    id("net.neoforged.moddev.legacyforge")
}

legacyForge {
    version = prop("minecraft_version") + "-" + prop("forge_version")
}

mixin {
    config("slipstream.mixins.json")
}

dependencies {
    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")
    implementation("jarJar"("io.github.llamalad7:mixinextras-forge:0.4.1")!!)
}

tasks.register<BetterRemapJar>("remapJar") {
    config(tasks.named<RemapJar>("reobfJar"))
    input = tasks.jar.flatMap { it.archiveFile }

    manifest.attributes(
        "MixinConfigs" to "slipstream.mixins.json",
    )
}

tasks.named<RemapJar>("reobfJar") {
    enabled = false
    finalizedBy(tasks.named("remapJar"))
}

apply(plugin = "slipstream-forge-common")
