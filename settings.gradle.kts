@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = ("argo-project")

includeBuild("build-conventions")
includeBuild("argo-plugin")
includeBuild("use-cases")

