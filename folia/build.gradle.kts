plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

val versionStr = (System.getenv("VERSION")?: "v1.0.0").removePrefix("v")

group = "com.funniray.minimap"
version = versionStr

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    // Main Dependencies
    compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")
    implementation(project(":common"))

    // Common Dependencies
    implementation("org.spongepowered:configurate-core:4.1.2")
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("net.kyori:adventure-api:4.10.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.1")
    implementation("net.kyori:adventure-text-minimessage:4.10.0")
    implementation("net.kyori:adventure-nbt:4.15.0")
}

val javaTarget = 21
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
    if (JavaVersion.current() < JavaVersion.toVersion(javaTarget)) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaTarget))
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        relocate("org.spongepowered.configurate", "com.funniray.minimap.configurate")
        relocate("net.kyori", "com.funniray.minimap.kyori")
        relocate("io.leangen.geantyref", "com.funniray.minimap.geantyref")
        exclude("com/google/gson/**")
        exclude("org/apache/commons/**")
        exclude("org/yaml/snakeyaml/**")
        archiveBaseName.set("${rootProject.name}-folia")
        archiveClassifier.set("")
        doLast {
            copy {
                from(archiveFile)
                into("${rootProject.projectDir}/build")
            }
        }
    }
}

bukkit {
    name = "MinimapControl"
    main = "com.funniray.minimap.folia.FoliaMinimap"
    authors = listOf("funniray")
    description = "Control minimap settings from server-side software"

    apiVersion = "1.21"
    softDepend = listOf("viaversion")
}