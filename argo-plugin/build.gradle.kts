@file:Suppress("UnstableApiUsage")


plugins {
    id("argo.plugin-conventions")
}

dependencies {
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.compiler)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.combinatorics)
}

gradlePlugin {
    website.set("https://github.com/LeoFuso/argo")
    vcsUrl.set("https://github.com/LeoFuso/argo")
    plugins {
        create("argo") {
            displayName = "Argo"
            id = project.group.toString()
            description = project.extra["local.description"] as String
            implementationClass = "io.github.leofuso.argo.plugin.ArgoPlugin"
            tags.set(
                listOf(
                    "avro",
                    "kafka",
                    "schema-registry",
                    "confluent",
                    "java",
                    "code generation"
                )
            )
        }
    }
}

tasks.test {
    dependsOn(
        projects.columbaCli
            .dependencyProject
            .tasks
            .publishToMavenLocal
    )
}

sonar {
    properties {
        property(
            "sonar.exclusions",
            """
                **/src/**/ColumbaInvoker.kt,
                **/src/**/ColumbaWorkAction.kt
            """.trimIndent().replace("\n", "")
        )
    }
}
