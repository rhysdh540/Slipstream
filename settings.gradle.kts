pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(":b3d") {
        fun add(mcVersion: String, vararg loaders: String) =
            loaders.forEach { version("$mcVersion-$it", mcVersion) }

        add("1.20.1", "fabric", "forge")
        add("1.21.1", "neoforge")
        add("1.21.11", "fabric", "neoforge")
        add("26.3", "fabric", "neoforge")

        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "Slipstream"

include("ornithe")
