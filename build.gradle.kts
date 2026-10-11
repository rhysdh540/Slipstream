plugins {
    id("java-library")
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
