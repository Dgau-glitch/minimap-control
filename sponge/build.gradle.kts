import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin") version "2.0.2"
    id("com.gradleup.shadow") version "9.3.2"
}

repositories {
    mavenCentral()
}

val versionStr = (System.getenv("VERSION")?: "v1.0.0").removePrefix("v")

group = "com.funniray.minimap"
version = versionStr

sponge {
    apiVersion("8.1.0")
    license("All Rights Reserved")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version(versionStr)
    }
    plugin("minimap") {
        displayName("Minimap")
        entrypoint("com.funniray.minimap.sponge.SpongeMinimap")
        description("Minimap extension for server side plugins")
        links {
// homepage("https://spongepowered.org")
// source("https://spongepowered.org/source")