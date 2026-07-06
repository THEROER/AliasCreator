plugins {
    // Applies the right Loom for the target, Minecraft + mappings + loader +
    // fabric-api, the Java toolchain, and the MagicUtils fabric bundle (with the
    // correct classifier + jar-in-jar). No Loom/obf details leak into this file.
    id("magicutils.consumer-fabric")
}

base {
    archivesName = "aliascreator-fabric"
}

magicutilsConsumer {
    devServer {
        modrinth("luckperms") {
            fabric("mc1.21" to "v5.5.17-fabric", "mc26" to "v5.5.57-fabric")
        }
    }
}

dependencies {
    implementation(project(":common"))
}

val magicutilsVersion = project.property("magicutils_version") as String
tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("magicutils_version", magicutilsVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version, "magicutils_version" to magicutilsVersion)
    }
}

tasks.named<Jar>("jar") {
    from(project(":common").sourceSets["main"].output)
}
