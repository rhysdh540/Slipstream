plugins {
    id("java")
    id("net.fabricmc.fabric-loom-remap")
    id("ploceus")
}

val mc = providers.gradleProperty("ornithe_minecraft").getOrElse("1.8.9")
val feather = providers.gradleProperty("ornithe_feather").getOrElse("2")

base.archivesName = "slipstream-$mc"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.compileJava {
    options.release = 17
}

repositories {
    exclusiveContent {
        forRepository { mavenCentral() }
        filter { includeGroup("org.lwjgl") }
    }
    maven("https://maven.ornithemc.net/releases")
    maven("https://maven.cloverclient.com/releases")
    maven("https://maven.axolotlclient.com/releases")
}

configurations.all {
    exclude(group = "org.lwjgl.lwjgl")
}

ploceus {
    setIntermediaryGeneration(2)
}

loom {
    runs.named("client") {
        programArgs.addAll(listOf("--userProperties", "{}"))
        if (!providers.gradleProperty("ornithe_off_main").isPresent) jvmArguments.add("-XstartOnFirstThread")
        if (providers.gradleProperty("ornithe_glfw").isPresent) systemProperties.put("legacy_lwjgl3.use_sdl", "false")
    }
}

val shade = configurations.create("shade") { isTransitive = false }

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings(ploceus.featherMappings(feather))
    modImplementation("net.fabricmc:fabric-loader:0.19.5")

    modCompileOnly("pl.tomgirl:pylon:0.2.1")
    compileOnly("org.lwjgl:lwjgl-glfw:3.4.2") { isTransitive = false }
    if (providers.gradleProperty("ornithe_lwjgl3").orNull == "legacy") {
        modLocalRuntime("io.github.moehreag:legacy-lwjgl3:1.4.9")
        ploceus.dependOsl("0.22.0")
    } else {
        modLocalRuntime("pl.tomgirl:pylon:0.2.1")
    }

    implementation(project(":"))
    shade(project(":"))

    providers.gradleProperty("ornithe_test_jar").orNull?.let { modLocalRuntime(files(it)) }
}

if (providers.gradleProperty("ornithe_test_jar").isPresent) {
    sourceSets.main {
        java.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
    }
}

tasks.jar {
    dependsOn(shade)
    from(provider { shade.map { zipTree(it) } }) { exclude("META-INF/MANIFEST.MF") }
}

tasks.processResources {
    val version = project.version
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.remapJar {
    destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
}
