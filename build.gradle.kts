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

    register("check") {
        group = "verification"
        description = "Delegates task-action to argo-plugin build."
        dependsOn(gradle.includedBuild("argo-plugin").task(":pluginUnderTestMetadata"))
        dependsOn(gradle.includedBuild("argo-plugin").task(":validatePlugins"))
        dependsOn(gradle.includedBuild("argo-plugin").task(":check"))
    }

    register("publish") {
        group = "publishing"
        description = "Delegates task-action to argo-plugin build."
        dependsOn(gradle.includedBuild("argo-plugin").task(":publishPlugins"))
    }

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}
