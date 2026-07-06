plugins {
    // java-library + shadow + Paper API + MagicUtils modules for the active
    // target. Bukkit's API is stable across the whole 1.21.x .. 26.2 range.
    id("magicutils.consumer-bukkit")
}

base {
    archivesName = "aliascreator-bukkit"
}

magicutilsConsumer {
    implementation("magicutils-bukkit", "magicutils-config-yaml")
    devServer {
        modrinth("luckperms") {
            paper("mc1.21" to "v5.5.17-bukkit", "mc26" to "v5.5.53-bukkit")
        }
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.1")
    implementation(project(":common"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
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
    relocate("com.fasterxml.jackson", "dev.ua.theroer.aliascreator.libs.jackson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
