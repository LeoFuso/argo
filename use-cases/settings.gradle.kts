@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {

    pluginManagement {
        repositories {
            google()
            mavenLocal()
            mavenCentral()
            gradlePluginPortal()
        }
    }

    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(
                files("../gradle/libs.versions.toml")
            )
        }
    }
}
rootProject.name = ("use-cases")
include("external-tools")
include("simple-kotlin")
include("simple-java")
include("kotlin-complete")

