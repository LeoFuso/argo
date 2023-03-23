@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = ("argo")

includeBuild("build-conventions")

if (System.getenv("CI") == null) {
    includeBuild("use-cases")
}

include("argo-plugin")
include("columba-cli")

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    if (System.getenv("CI") != null || gradle.startParameter.isBuildScan) {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
            publishAlways()
        }
    }
}
