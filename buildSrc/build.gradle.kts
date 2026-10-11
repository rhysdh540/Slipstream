plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    fun m(u: String, g: String) = maven(u) {
        content { includeGroupAndSubgroups(g) }
    }
    m("https://maven.neoforged.net/releases", "net.neoforged")
    m("https://maven.fabricmc.net", "net.fabricmc")
    m("https://maven.wagyourtail.xyz/snapshots", "xyz.wagyourtail")
    m("https://maven.wagyourtail.xyz/releases", "xyz.wagyourtail")
}

dependencies {
    implementation("xyz.wagyourtail:manifold-gradle:1.1.1-SNAPSHOT")
    implementation("net.neoforged:moddev-gradle:2.0.148")
    implementation("net.fabricmc:fabric-loom:1.18.4")
    implementation("net.fabricmc:tiny-remapper:0.11.1")
}
