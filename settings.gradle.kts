rootProject.name = "Debt-Tracker"

pluginManagement {
    repositories {
        google {
            content {
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Lets Gradle auto-download JVM toolchains (incl. JetBrains Runtime for
    // desktopApp:hotRun/Compose Hot Reload) via the Foojay Disco API.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google {
            content {
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        mavenCentral()
    }
}
include(":sharedUI")
include(":androidApp")
include(":desktopApp")
include(":webApp")

