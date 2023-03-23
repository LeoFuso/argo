plugins {
    base
    idea
    id("argo.release-conventions")
}

tasks {

    register("publish-plugin") {
        group = "publishing"
        description = "Publishes 'argo' to the Gradle Plugin Portal."
        dependsOn(":argo-plugin:publishPlugins")
    }

    register("publish-cli") {
        group = "publishing"
        description = "Publishes 'columba-cli' to Sonatype and releases it."
        dependsOn(
            ":columba-cli:clean",
            ":columba-cli:publishToSonatype",
            ":closeAndReleaseSonatypeStagingRepository"
        )
    }

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}

project(":columba-cli")
    .beforeEvaluate {

        group = "io.github.leofuso.columba"
        version = System.getProperty("global.version")
        extra["local.description"] =
            """
            A command line interface that supports Java code generation from JSON schema declaration files(.avsc),
            JSON protocol declaration files(.avpr), and Avro IDL(.avdl) source files.
        """.trimIndent()

    }

project(":argo-plugin")
    .beforeEvaluate {

        group = "io.github.leofuso.argo"
        version = System.getProperty("global.version")
        extra["local.description"] =
            """
                A Gradle Plugin that supports Java code generation from JSON schema declaration files(.avsc),
                JSON protocol declaration files(.avpr), and Avro IDL(.avdl) source files
            """.trimIndent()

    }


