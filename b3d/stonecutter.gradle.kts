plugins {
    id("dev.kikugie.stonecutter")
    id("xyz.wagyourtail.manifold") version "1.1.1-SNAPSHOT"
    id("net.fabricmc.fabric-loom") version "1.18.4" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.18.4" apply false
}

stonecutter active "26.3"

manifold {
    version = "2026.1.15"
    pluginArgs.add("--no-bootstrap")

    subprojectPreprocessor {
        for ((p, v) in stonecutter.versions) {
            subproject(project(p)) {
                val (major, minor) = (v.removePrefix("1.") + ".0").split('.')
                property("MC", "$major.${minor.padStart(2, '0')}")
            }
        }

        stonecutter.current?.let { ideActiveSubproject = it.project }
    }
}
