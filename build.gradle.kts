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

group = "dev.stoshe.plots"
val pluginVersion = (findProperty("version") as? String)
    ?.takeIf { it.isNotBlank() && it != "unspecified" }
    ?: "1.2.1"
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

// Protection mixins are now provided by the shared TaleGuard bridge
// (consumed at runtime via the reflective hook registry). Plots no longer
// ships its own early-bridge source set.

tasks.withType<JavaCompile> {
    options.release.set(javaLanguageVersion)
}

repositories {
    mavenCentral()
    maven(url = "https://repo.spongepowered.org/repository/maven-public/")
    maven(url = "https://maven.fabricmc.net/")
    flatDir { dirs("libs") }
}

dependencies {
    compileOnly(files(hytaleServerJar))
    compileOnly(files("libs/hylograms-1.1.1.jar"))
    compileOnly(files("libs/TaleGuard-1.0.0.jar"))
    compileOnly(files("libs/Placeholder-1.0.0.jar"))
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
