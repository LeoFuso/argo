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

        val lib = getDependency("columba-cli")
        group = lib.group
        version = lib.versionConstraint.toString()
        extra["local.description"] =
            """
            A command line interface that supports Java code generation from JSON schema declaration files(.avsc),
            JSON protocol declaration files(.avpr), and Avro IDL(.avdl) source files.
        """.trimIndent()

    }

project(":argo-plugin")
    .beforeEvaluate {

        val lib = getDependency("argo")
        group = lib.group
        version = lib.versionConstraint.toString()
        extra["local.description"] =
            """
                A Gradle Plugin that supports Java code generation from JSON schema declaration files(.avsc),
                JSON protocol declaration files(.avpr), and Avro IDL(.avdl) source files
            """.trimIndent()

    }

fun getDependency(name: String) =
    project.extensions.getByType<VersionCatalogsExtension>()
    .named("libs")
    .findLibrary(name)
    .get()
    .get()


