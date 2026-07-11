plugins {
    // java-library + shadow + Velocity API + MagicUtils modules for the active
    // target. Velocity's API is decoupled from the Minecraft version.
    id("magicutils.consumer-velocity")
}

base {
    archivesName = "commandflow-velocity"
}

description = "CommandFlow proxy module for Velocity"

magicutilsConsumer {
    // magicutils-messaging comes transitively through :common; the platform
    // module gives us the proxy runtime (VelocityBootstrap, command registry).
    implementation("magicutils-velocity")
}

dependencies {
    implementation(project(":common"))
}

tasks.processResources {
    val props = mapOf("version" to project.version, "description" to project.description)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("velocity-plugin.json")) {
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
    // With magicutils_embed=false the consumer-velocity plugin puts MagicUtils on
    // compileOnly and strips dev/ua/theroer/magicutils + jackson from the shadow
    // jar, so this proxy plugin is thin and expects the MagicUtils velocity-bundle
    // installed alongside it (declared as a dependency in velocity-plugin.json).
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
