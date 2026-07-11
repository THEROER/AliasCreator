plugins {
    // java-library + shadow + Paper API + MagicUtils modules for the active
    // target. Bukkit's API is stable across the whole 1.21.x .. 26.2 range.
    id("magicutils.consumer-bukkit")
}

base {
    archivesName = "commandflow-bukkit"
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
    // jackson is provided at runtime by the MagicUtils bukkit-bundle plugin
    // (its ConfigManager owns the YAML factories). Bundling and relocating our
    // own copy produced two incompatible jackson packages and a ClassCastException
    // under Paper's isolated classloaders, so we don't ship jackson here.
    implementation(project(":common"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
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
    // MagicUtils runs as a separate server plugin (paper-plugin dependency,
    // join-classpath) and owns its own relocated jackson. With
    // `magicutils_embed=false` (EmbedMode.EXTERNAL) the consumer-bukkit plugin
    // puts the MagicUtils modules on compileOnly and strips
    // dev/ua/theroer/magicutils from the shadow jar itself, so no manual exclude
    // is needed here and jackson never reaches this jar.
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
