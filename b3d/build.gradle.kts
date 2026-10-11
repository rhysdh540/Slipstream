import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("java")
}

val mc = stonecutter.current.version
val unobfuscated = stonecutter.eval(mc, ">=26")
val blaze3dEncoder = stonecutter.eval(mc, ">=1.21.5")

val minecraftRange = when {
    unobfuscated -> ">=26.1"
    blaze3dEncoder -> ">=1.21.5 <=1.21.11"
    else -> ">=1.19 <=1.21.4"
}
val javaVersion = if (unobfuscated) 25 else if (stonecutter.eval(mc, ">=1.20.5")) 21 else 17
apply(plugin = if (unobfuscated) "net.fabricmc.fabric-loom" else "net.fabricmc.fabric-loom-remap")

base.archivesName = "slipstream-$mc"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

manifold.preprocessor {
    sourceSet("main")
}

if (System.getProperty("idea.sync.active") == "true" && !stonecutter.current.isActive) {
    afterEvaluate { sourceSets.main { java.setSrcDirs(emptyList<File>()) } }
}

val shade = configurations.create("shade") { isTransitive = false }

dependencies {
    "minecraft"("com.mojang:minecraft:${providers.gradleProperty("minecraftVersion").orNull?.takeIf { unobfuscated } ?: mc}")
    if (unobfuscated) {
        implementation("net.fabricmc:fabric-loader:0.19.5")
        compileOnly("org.lwjgl:lwjgl-glfw:3.4.3") { isTransitive = false }
        compileOnly("org.lwjgl:lwjgl-sdl:3.4.3") { isTransitive = false }
    } else {
        "mappings"(project.the<LoomGradleExtensionAPI>().officialMojangMappings())
        "modImplementation"("net.fabricmc:fabric-loader:0.19.5")
    }
    implementation(project(":"))
    shade(project(":"))
}

tasks.jar {
    dependsOn(shade)
    from(provider { shade.map { zipTree(it) } }) { exclude("META-INF/MANIFEST.MF") }
}

tasks.withType<Sync>().configureEach {
    if (name.startsWith("stonecutterGenerate")) exclude("**/build.properties")
}

tasks.processResources {
    val properties = mapOf(
        "version" to project.version,
        "minecraft" to minecraftRange,
        "java" to javaVersion,
    )
    inputs.properties(properties)
    filesMatching(listOf("fabric.mod.json", "slipstream.mixins.json")) {
        expand(properties)
    }
}

tasks.named<AbstractArchiveTask>(if (unobfuscated) "jar" else "remapJar") {
    destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
}
