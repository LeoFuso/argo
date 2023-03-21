plugins {
    idea
}

tasks {

    register("clean") {
        group = "build"
        description = "Delegates task-action to all included builds."

        val cleanTasks = gradle.includedBuilds.map { it.task(":clean") }.toTypedArray()
        dependsOn(cleanTasks)
    }

    register("build") {
        group = "build"
        description = "Delegates task-action to relevant included builds."
        dependsOn(
            gradle.includedBuild("columba-cli").task(":build"),
            gradle.includedBuild("argo-plugin").task(":build")
        )
    }

    register("check") {
        group = "verification"
        description = "Delegates task-action to relevant included builds."
        dependsOn(
            gradle.includedBuild("argo-plugin").task(":pluginUnderTestMetadata"),
            gradle.includedBuild("argo-plugin").task(":validatePlugins"),
            gradle.includedBuild("argo-plugin").task(":check"),
            gradle.includedBuild("columba-cli").task(":check")
        )
    }

    register("publish-plugin") {
        group = "publishing"
        description = "Delegates task-action to argo-plugin build."
        dependsOn(gradle.includedBuild("argo-plugin").task(":publishPlugins"))
    }

    register("publish-cli") {
        group = "publishing"
        description = "Delegates task-action to columba-cli build."
        dependsOn(
            gradle.includedBuild("columba-cli").task(":clean"),
            gradle.includedBuild("columba-cli").task(":publishToSonatype"),
            gradle.includedBuild("columba-cli").task(":closeAndReleaseSonatypeStagingRepository"),
        )
    }

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}
