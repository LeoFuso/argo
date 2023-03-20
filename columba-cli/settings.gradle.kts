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
rootProject.name = ("columba-cli")
gradle.rootProject {
    group = "io.github.leofuso.columba"
    version = System.getProperty("global.version")
    extra["local.description"] =
        """
            A command line interface that supports code generation for JSON schema declaration files(.avsc),
            JSON protocol declaration files(.avpr), and Avro IDL(.avdl) files.
        """.trimIndent()
}
