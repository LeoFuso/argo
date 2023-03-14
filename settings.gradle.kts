@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = ("argo-project")

includeBuild("build-conventions")
includeBuild("argo-plugin")
includeBuild("columba-cli")

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
