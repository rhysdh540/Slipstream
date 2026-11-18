import com.gtnewhorizons.retrofuturagradle.mcp.MCPTasks
import com.gtnewhorizons.retrofuturagradle.minecraft.MinecraftTasks
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import com.gtnewhorizons.retrofuturagradle.util.Distribution

plugins {
    id("com.gtnewhorizons.retrofuturagradle") version("2.0.2")
}

val lwjgl3ify = "com.github.GTNewHorizons:lwjgl3ify:3.1.0"
val unimixins = "io.github.legacymoddingmc:unimixins:0.3.1"
val hodgepodge = "com.github.GTNewHorizons:Hodgepodge:2.7.206"

repositories {
    maven("https://nexus.gtnewhorizons.com/repository/public/") {
        content {
            includeGroup("com.github.GTNewHorizons")
            includeGroup("io.github.legacymoddingmc")
            includeGroup("com.gtnewhorizons.retrofuturabootstrap")
        }
    }
}

minecraft {
    mcVersion = "1.7.10"
    mainLwjglVersion = 3
    lwjgl3Version = "3.4.3"
    lwjgl3Bindings = listOf("glfw", "openal", "opengl", "jemalloc", "stb", "tinyfd", "sdl", "spng")
    javaCompatibilityVersion = 17
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.compileJava {
    options.release = 17
}

val shade = configurations.create("shade") {
    isTransitive = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}
val lwjgl3Runtime = configurations.create("lwjgl3Runtime") {
    extendsFrom(configurations.runtimeClasspath.get())
    exclude(group = "dev.rdh", module = "Slipstream")
}
val lwjgl3Patches = configurations.create("lwjgl3Patches") { isTransitive = false }

dependencies {
    implementation(project(":"))
    shade(project(":"))

    compileOnly("$lwjgl3ify:dev") { isTransitive = false }

    annotationProcessor("org.ow2.asm:asm-debug-all:5.0.4")
    annotationProcessor("com.google.guava:guava:24.1.1-jre")
    annotationProcessor("com.google.code.gson:gson:2.13.2")
    annotationProcessor("$unimixins:dev")
    implementation(modUtils.enableMixins("$unimixins:dev", "mixins.slipstream.refmap.json"))

    lwjgl3Runtime(lwjgl3ify)
    lwjgl3Runtime(hodgepodge)
    lwjgl3Patches("$lwjgl3ify:forgePatches")
}

tasks.jar {
    from(shade.elements.map { jars -> jars.map { zipTree(it.asFile) } }) { exclude("META-INF/MANIFEST.MF") }
    manifest.attributes(
        "Lwjgl3ify-Aware" to true,
        "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
        "MixinConfigs" to "mixins.slipstream.json",
        "ForceLoadAsMod" to true,
        "FMLCorePluginContainsFMLMod" to true,
    )
}

tasks.reobfJar {
    archiveBaseName = "slipstream-${minecraft.mcVersion.get()}-forge"
    destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
}

tasks.register("runClientLwjgl3", RunMinecraftTask::class.java, Distribution.CLIENT).configure {
    setup(project)
    group = "Modded Minecraft"
    val mcpTasks = project.extensions.getByType<MCPTasks>()
    val mcTasks = project.extensions.getByType<MinecraftTasks>()
    lwjglVersion = 3
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
    classpath(lwjgl3Patches)
    classpath(mcpTasks.taskPackageMcLauncher)
    classpath(mcpTasks.taskPackagePatchedMc)
    classpath(mcpTasks.patchedConfiguration)
    classpath(tasks.jar)
    classpath(lwjgl3Runtime)
    dependsOn(mcpTasks.launcherSources.classesTaskName, mcTasks.taskDownloadVanillaAssets, mcpTasks.taskPackagePatchedMc, tasks.jar)
    mainClass = "GradleStart"
    systemProperty("gradlestart.bouncerClient", "com.gtnewhorizons.retrofuturabootstrap.MainStartOnFirstThread")
    extraJvmArgs.addAll(
        "-XstartOnFirstThread",
        "-Dfile.encoding=UTF-8",
        "-Djava.system.class.loader=com.gtnewhorizons.retrofuturabootstrap.RfbSystemClassLoader",
        "--enable-native-access=ALL-UNNAMED",
    )
    for (module in listOf("java.base/java.io", "java.base/java.lang.invoke", "java.base/java.lang.ref",
            "java.base/java.lang.reflect", "java.base/java.lang", "java.base/java.net.spi", "java.base/java.net",
            "java.base/java.nio.channels", "java.base/java.nio.charset", "java.base/java.nio.file", "java.base/java.nio",
            "java.base/java.text", "java.base/java.time.chrono", "java.base/java.time.format",
            "java.base/java.time.temporal", "java.base/java.time.zone", "java.base/java.time",
            "java.base/java.util.concurrent.atomic", "java.base/java.util.concurrent.locks", "java.base/java.util.jar",
            "java.base/java.util.zip", "java.base/java.util", "java.base/jdk.internal.loader",
            "java.base/jdk.internal.misc", "java.base/jdk.internal.ref", "java.base/jdk.internal.reflect",
            "java.base/sun.nio.ch", "java.desktop/com.sun.imageio.plugins.png", "java.desktop/sun.awt.image",
            "java.desktop/sun.awt", "java.desktop/sun.lwawt.macosx")) {
        extraJvmArgs.addAll("--add-opens", "$module=ALL-UNNAMED")
    }
}

tasks.processResources {
    val version = project.version
    inputs.property("version", version)
    filesMatching("mcmod.info") {
        expand("version" to version)
    }
}
