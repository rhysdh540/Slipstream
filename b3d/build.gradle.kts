plugins {
    id("java")
}

val (mc, platform) = stonecutter.current.project.split('-', limit = 2)
ext["minecraft_version"] = mc
ext["platform"] = platform

val blaze3dEncoder = stonecutter.eval(mc, ">=1.21.5")
val forgelike = platform != "fabric"

val (fabricRange, forgeRange) = when {
    unobfuscated -> ">=26.1" to "[26.1,)"
    blaze3dEncoder -> ">=1.21.5 <=1.21.11" to "[1.21.5,1.21.11]"
    platform == "forge" -> "" to "[$mc]"
    forgelike -> "" to "[1.21,1.21.4]"
    else -> ">=1.19 <=1.21.4" to ""
}
val javaVersion = if (unobfuscated) 25 else if (stonecutter.eval(mc, ">=1.20.5")) 21 else 17

base.archivesName = "slipstream-$mc-$platform"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

apply(plugin = "slipstream-$platform")

manifold.preprocessor {
    sourceSet("main")
}

if (System.getProperty("idea.sync.active") == "true" && !stonecutter.current.isActive) {
    afterEvaluate { sourceSets.main { java.setSrcDirs(emptyList<File>()) } }
}

val shade = configurations.create("shade") { isTransitive = false }

dependencies {
    implementation(project(":"))
    shade(project(":"))
    if (unobfuscated) {
        compileOnly("org.lwjgl:lwjgl-glfw:3.4.3") { isTransitive = false }
        compileOnly("org.lwjgl:lwjgl-sdl:3.4.3") { isTransitive = false }
    }
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
        "minecraft" to fabricRange,
        "minecraft_forge" to forgeRange,
        "java" to javaVersion,
        "required" to if (platform == "forge") "mandatory = true" else "type = \"required\"",
        "display_test" to if (platform == "forge") "displayTest = \"IGNORE_ALL_VERSION\"" else "",
    )
    inputs.properties(properties)
    filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "slipstream.mixins.json")) {
        expand(properties)
    }
}

jarOutputTask {
    destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
}
