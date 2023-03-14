@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("../build-conventions")
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(
                files("$rootDir/libs.versions.toml")
            )
        }
    }
}
rootProject.name = ("columba-cli")
