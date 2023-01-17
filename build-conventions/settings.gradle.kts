@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    versionCatalogs {
        create("libs") {
            from(
                files("$rootDir/libs.versions.toml")
            )
        }
    }
}
rootProject.name = ("build-conventions")
