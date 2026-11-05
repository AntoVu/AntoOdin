rootProject.name = "AntoOdin"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://jitpack.io")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
    }

    val loom_version: String by settings
    val kotlin_version: String by settings

    plugins {
        id("net.fabricmc.fabric-loom") version loom_version
        kotlin("jvm") version kotlin_version
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

// One source tree, one jar per Minecraft version. Git holds the 26.2 code; 26.3 code sits in //? comment blocks
stonecutter {
    create(rootProject) {
        versions("26.2", "26.3")
        vcsVersion = "26.2"
    }
}
