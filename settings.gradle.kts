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
