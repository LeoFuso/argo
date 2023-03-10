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

/* Unecessary build */
if (System.getenv("CI") == null) {
    includeBuild("use-cases")
}


plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    if (System.getenv("CI") != null) {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
            publishAlways()
        }
    }
}
