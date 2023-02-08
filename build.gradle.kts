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

    register("test") {
        group = "verification"
        description = "Delegates task-action to argo-plugin build."
        dependsOn(gradle.includedBuild("argo-plugin").task(":test"))
    }

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}
