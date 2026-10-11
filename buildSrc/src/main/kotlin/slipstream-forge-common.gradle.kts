import org.gradle.internal.extensions.stdlib.capitalized

plugins {
    id("java")
}

forge {
    mods.maybeRegister("slipstream") {
        modSourceSets.add(sourceSets.main)
        modSourceSets.add(project(":").sourceSets.main)
    }

    runs {
        maybeRegister("client") { client() }

        configureEach {
            ideName = "${prop("platform").capitalized()} ${name.capitalized()}: ${prop("minecraft_version")}"
        }
    }
}

tasks.processResources {
    exclude("fabric.mod.json")
}
