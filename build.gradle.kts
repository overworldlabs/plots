import groovy.json.JsonOutput
import groovy.yaml.YamlSlurper
import org.apache.tools.ant.filters.BaseFilterReader
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.api.tasks.compile.JavaCompile
import java.io.Reader

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.apache.groovy:groovy-yaml:5.0.3")
    }
}

plugins {
    id("java")
}

group = "com.overworldlabs.plots"
val pluginVersion = (findProperty("version") as? String)
    ?.takeIf { it.isNotBlank() && it != "unspecified" }
    ?: "1.1.1"
version = pluginVersion

val javaVersion = (findProperty("javaVersion") as? String) ?: "25"
val javaLanguageVersion = javaVersion.substringBefore('.').toInt()
val patchline = (findProperty("patchline") as? String) ?: "release"
val includesPack = ((findProperty("includesPack") as? String)?.toBoolean()) ?: false
val loadUserMods = ((findProperty("loadUserMods") as? String)?.toBoolean()) ?: false

val hytaleHome: String by extra {
    if (project.hasProperty("hytale_home")) {
        project.findProperty("hytale_home") as String
    } else {
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        when {
            os.isWindows -> "${System.getProperty("user.home")}/AppData/Roaming/Hytale"
            os.isMacOsX -> "${System.getProperty("user.home")}/Library/Application Support/Hytale"
            os.isLinux -> {
                val flatpakPath = "${System.getProperty("user.home")}/.var/app/com.hypixel.HytaleLauncher/data/Hytale"
                if (file(flatpakPath).exists()) {
                    flatpakPath
                } else {
                    "${System.getProperty("user.home")}/.local/share/Hytale"
                }
            }

            else -> throw GradleException(
                "Could not detect Hytale install. Set -Phytale_home=/path/to/Hytale."
            )
        }
    }
}

if (hytaleHome.isBlank()) {
    throw GradleException("Could not detect Hytale install. Set -Phytale_home=/path/to/Hytale.")
} else if (!file(hytaleHome).exists()) {
    throw GradleException("Hytale path not found: $hytaleHome")
}

val hytaleServerJar = "$hytaleHome/install/$patchline/package/game/latest/Server/HytaleServer.jar"
val hytaleAssets = "$hytaleHome/install/$patchline/package/game/latest/Assets.zip"

if (!file(hytaleServerJar).exists()) {
    throw GradleException("HytaleServer.jar not found at: $hytaleServerJar")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
    }
}

val earlyBridge by sourceSets.creating {
    java.srcDir("src/earlybridge/java")
    resources.srcDir("src/earlybridge/resources")
}

// Mixin 0.8.x + ASM can silently no-op with very new classfile versions.
// Keep early bridge bytecode conservative for runtime compatibility.
val earlyBridgeBytecodeVersion = 21

tasks.withType<JavaCompile> {
    options.release.set(javaLanguageVersion)
}

tasks.named<JavaCompile>(earlyBridge.compileJavaTaskName) {
    options.release.set(earlyBridgeBytecodeVersion)
}

repositories {
    mavenCentral()
    maven(url = "https://repo.spongepowered.org/repository/maven-public/")
    maven(url = "https://maven.fabricmc.net/")
    flatDir { dirs("libs") }
}

dependencies {
    compileOnly(files(hytaleServerJar))
    add("${earlyBridge.name}CompileOnly", files(hytaleServerJar))
    add("${earlyBridge.name}Implementation", "org.ow2.asm:asm:9.7")
    add("${earlyBridge.name}Implementation", "org.ow2.asm:asm-analysis:9.7")
    add("${earlyBridge.name}Implementation", "org.ow2.asm:asm-commons:9.7")
    add("${earlyBridge.name}Implementation", "org.ow2.asm:asm-tree:9.7")
    add("${earlyBridge.name}Implementation", "org.ow2.asm:asm-util:9.7")
    add("${earlyBridge.name}Implementation", "net.fabricmc:sponge-mixin:0.16.5+mixin.0.8.7")
    add("${earlyBridge.name}Implementation", "org.ow2.sat4j:org.ow2.sat4j.core:2.3.6")
    add("${earlyBridge.name}Implementation", "org.ow2.sat4j:org.ow2.sat4j.pb:2.3.6")
    add("${earlyBridge.name}Implementation", "com.google.guava:guava:33.2.1-jre")
    add("${earlyBridge.name}Implementation", "com.google.code.gson:gson:2.10.1")
    compileOnly(files("libs/hylograms-1.1.1.jar"))
    compileOnly(fileTree("libs/economy") { include("*.jar") })
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.1")
    implementation("com.mysql:mysql-connector-j:8.4.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")
    implementation("org.postgresql:postgresql:42.7.4")
}

class YAMLToJSONReader(r: Reader) : BaseFilterReader(r) {
    private var buffer: CharArray? = null
    private var index = 0

    override fun read(): Int {
        if (this.index > -1) {
            if (buffer == null) {
                @Suppress("UNCHECKED_CAST")
                val yaml = YamlSlurper().parseText(`in`.readText()) as MutableMap<String, Any>
                buffer = JsonOutput.prettyPrint(JsonOutput.toJson(yaml)).toCharArray()
            }

            if (this.index < this.buffer!!.size) {
                return this.buffer!![this.index++].code
            }

            this.index = -1
        }

        return -1
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("includesPack", includesPack)

    filesMatching("manifest.json") {
        expand("version" to project.version)
    }

    filesMatching("manifest.yml") {
        name = "manifest.json"
        expand(inputs.properties)
        filter(YAMLToJSONReader::class.java)
    }
}

tasks.named<ProcessResources>(earlyBridge.processResourcesTaskName) {
    inputs.property("version", project.version)
    filesMatching("manifest.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("Plots")
    archiveVersion.set(project.version.toString())

    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }

    from("src/main/resources")
}

tasks.register<Jar>("earlyBridgeJar") {
    group = "build"
    description = "Builds the Plots earlyplugin mixin bridge jar."
    dependsOn(earlyBridge.classesTaskName)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("Plots-MixinBridge")
    archiveVersion.set(project.version.toString())
    from(earlyBridge.output)
    from({
        configurations[earlyBridge.runtimeClasspathConfigurationName]
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    manifest {
        attributes(
            "Implementation-Title" to "Plots-MixinBridge",
            "Implementation-Version" to project.version
        )
    }
}

val syncAssets = tasks.register<Copy>("syncAssets") {
    group = "hytale"
    description = "Sync assets generated in build back to source resources."

    from(sourceSets.main.get().output.resourcesDir?.absolutePath)
    into(sourceSets.main.get().resources.srcDirs.first().absolutePath)
    exclude("manifest.json")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

fun createServerArgs(): List<String> {
    val args = mutableListOf(
        "--allow-op",
        "--disable-sentry",
        "--assets=\"$hytaleAssets\""
    )

    val modsPaths = mutableListOf<String>()
    if (loadUserMods) {
        modsPaths.add("$hytaleHome/UserData/Mods")
    }

    if (includesPack) {
        modsPaths.add(sourceSets.main.get().output.resourcesDir?.parentFile?.absolutePath ?: "")
    }

    if (modsPaths.isNotEmpty()) {
        args.add("--mods=\"${modsPaths.joinToString(",")}\"")
    }
    return args
}

tasks.register<JavaExec>("runServer") {
    group = "hytale"
    description = "Runs local Hytale server using files from the game install."
    dependsOn("classes")
    finalizedBy(syncAssets)
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
    })

    mainClass.set("com.hypixel.hytale.Main")
    args = createServerArgs()
    classpath = files(
        hytaleServerJar,
        sourceSets.main.get().output.classesDirs,
        sourceSets.main.get().output.resourcesDir?.absolutePath
    )
    workingDir = file("run")
    standardInput = System.`in`
}
