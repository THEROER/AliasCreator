plugins {
    // java-library + shadow + BungeeCord API + MagicUtils modules for the active
    // target. Bungee's API is decoupled from the Minecraft version.
    id("magicutils.consumer-bungee")
}

base {
    archivesName = "commandflow-bungee"
}

description = "CommandFlow proxy module for BungeeCord/Waterfall"

magicutilsConsumer {
    // Bungee is always shaded (no EXTERNAL mode), so MagicUtils modules end up in
    // the fat jar. magicutils-messaging comes transitively through :common.
    api("magicutils-bungee")
}

dependencies {
    implementation(project(":common"))
}

tasks.processResources {
    val props = mapOf("version" to project.version, "description" to project.description)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("bungee.yml", "plugin.yml")) {
        expand(props)
    }
}

tasks.named<Jar>("jar") {
    archiveClassifier = "plain"
    from(project(":common").sourceSets["main"].output)
}

tasks.shadowJar {
    archiveClassifier = ""
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
