plugins {
    id("dev.kikugie.stonecutter")
    id("xyz.wagyourtail.manifold")
}

stonecutter active "26.3-fabric"

manifold {
    version = "2026.1.15"
    pluginArgs.add("--no-bootstrap")

    subprojectPreprocessor {
        for ((p, v) in stonecutter.versions) {
            subproject(project(p)) {
                val (major, minor) = (v.removePrefix("1.") + ".0").split('.')
                property("MC", "$major.${minor.padStart(2, '0')}")

                val platform = p.substringAfterLast('-')
                property(platform)
                if (platform != "fabric") property("forgelike")
            }
        }

        stonecutter.current?.let { ideActiveSubproject = it.project }
    }
}
