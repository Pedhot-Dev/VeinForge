plugins {
    kotlin("jvm") version "2.4.0"
    id("net.fabricmc.fabric-loom") version "1.17.11"
    id("io.freefair.lombok") version "8.6"
    id("com.gradleup.shadow") version "9.3.1"
}

val shadowModImpl by configurations.creating

val baseGroup: String by project
val mcVersion: String by project
val modVersion = project.version.toString()
val loomVersion: String by project
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
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    jvmToolchain(25)
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

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$kotlinLoaderVersion")
    compileOnly("org.notenoughupdates.moulconfig:modern-26.2:4.7.2")
    add("shadowModImpl", "org.notenoughupdates.moulconfig:modern-26.2:4.7.2")
    compileOnly("com.terraformersmc:modmenu:${modmenuVersion}")

    compileOnly("org.jetbrains:annotations:26.0.1")
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

tasks.jar {
    dependsOn(tasks.shadowJar)
    from(zipTree(tasks.shadowJar.get().archiveFile))
    archiveBaseName.set(modName)
    archiveClassifier.set("")
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

tasks.assemble.get().dependsOn(tasks.jar)

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
