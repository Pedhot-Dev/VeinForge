plugins {
    kotlin("jvm") version "2.2.21"
    id("fabric-loom") version "1.15.3"
    id("io.freefair.lombok") version "8.6"
    id("com.gradleup.shadow") version "9.3.1"
}

val shadowModImpl by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

val baseGroup: String by project
val mcVersion: String by project
val modVersion = project.version.toString()
val yarnMappings: String by project
val loaderVersion: String by project
val fabricApiVersion: String by project
val kotlinLoaderVersion: String by project
val modmenuVersion: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project
val modName: String by project

group = baseGroup

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

// Minecraft configuration:
loom {
    accessWidenerPath.set(file("src/main/resources/veinforge.accesswidener"))
}

// Dependencies:
repositories {
    maven("https://maven.notenoughupdates.org/releases") {
        content {
            includeGroup("org.notenoughupdates.moulconfig")
        }
    }
    maven("https://maven.fabricmc.net/")
    maven("https://repo.spongepowered.org/maven/")
    maven {
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/")
    }
}

dependencyLocking {
    lockAllConfigurations()
    lockMode.set(org.gradle.api.artifacts.dsl.LockMode.STRICT)
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("net.fabricmc:fabric-language-kotlin:$kotlinLoaderVersion")
    "shadowModImpl"("org.notenoughupdates.moulconfig:modern-1.21.11:4.3.0-beta")
    modCompileOnly("com.terraformersmc:modmenu:${modmenuVersion}")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("com.typesafe.akka:akka-actor_2.13:2.6.20")
}

// Tasks:

val lintDeprecation: Boolean = (findProperty("lintDeprecation") as String?)?.toBoolean() ?: false

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    if (lintDeprecation) {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

tasks.withType<Jar> {
    archiveBaseName.set(modName)
}

// Keep intermediate/dev jars out of build/libs to avoid confusion.
tasks.jar {
    archiveClassifier.set("dev")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    inputs.property("version", modVersion)
    inputs.property("mcversion", mcVersion)
    inputs.property("loaderVersion", loaderVersion)
    inputs.property("modid", modid)
    inputs.property("modName", modName)
    inputs.property("mixinGroup", mixinGroup)

    filesMatching(listOf("fabric.mod.json", "mixins.$modid.json", "version.properties")) {
        expand(inputs.properties)
    }

    rename("(.+_at.cfg)", "META-INF/$1")
}

tasks.shadowJar {
    configurations = listOf(shadowModImpl)
    archiveClassifier.set("shadow")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    relocate("io.github.notenoughupdates.moulconfig", "me.grish.veinforge.deps.moulconfig")
    mergeServiceFiles()
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)
    archiveBaseName.set(modName)
    archiveClassifier.set("")
}
tasks.assemble.get().dependsOn(remapJar)

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
