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
                files("../gradle/libs.versions.toml")
            )
        }
    }
}
rootProject.name = ("argo-plugin")
