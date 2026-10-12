plugins {
    id("java-library")
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
}

allprojects {
    group = "dev.rdh"
    version = "0.1.0"
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

tasks.compileJava {
    options.release = 17
}

dependencies {
    compileOnly(platform("org.lwjgl:lwjgl-bom:3.4.1"))
    compileOnly("org.lwjgl:lwjgl")
    compileOnly("org.lwjgl:lwjgl-opengl")
    compileOnly("org.lwjgl:lwjgl-vulkan")
    compileOnly("org.lwjgl:lwjgl-glfw")
    compileOnly("org.lwjgl:lwjgl-sdl:3.4.3") { isTransitive = false }
}

publishMods {
    changelog.set(providers.gradleProperty("changelog").orElse("Slipstream ${project.version}. macOS only."))
    type.set(STABLE)
    dryRun.set(providers.gradleProperty("publishDryRun").map(String::toBoolean).orElse(false))

    val modrinthOptions = modrinthOptions {
        projectId.set("XDGYMuZ6")
        accessToken.set(providers.environmentVariable("MODRINTH_PAT"))
        environment.set(CLIENT_ONLY)
    }

    gradle.projectsEvaluated {
        val builds = project(":b3d").subprojects + project(":ornithe")
        for (build in builds) {
            val ornithe = build.name == "ornithe"
            val minecraft = if (ornithe) providers.gradleProperty("ornithe_minecraft").getOrElse("1.8.9")
                else build.prop("minecraft_version")
            val loader = if (ornithe) "ornithe" else build.prop("platform")
            val jar = if (ornithe) build.tasks.named<AbstractArchiveTask>("remapJar")
                else build.jarOutputTask
            val (minimum, maximum) = when {
                ornithe -> "1.6.4" to "1.12.2"
                build.unobfuscated -> "26.1" to "latestRelease"
                minecraft == "1.21.11" -> "1.21.5" to "1.21.11"
                loader == "forge" -> minecraft to minecraft
                loader == "neoforge" -> "1.21" to "1.21.4"
                else -> "1.19" to "1.21.4"
            }
            modrinth("modrinth-${minecraft}-${loader}") {
                from(modrinthOptions)
                file.set(jar.flatMap { it.archiveFile })
                version.set("${project.version}-${minecraft}-${loader}")
                displayName.set("Slipstream ${project.version} ($minecraft, $loader)")
                modLoaders.add(loader)
                minecraftVersionRange {
                    start.set(minimum)
                    end.set(maximum)
                }
            }
        }
    }
}
