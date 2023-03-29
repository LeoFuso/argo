@file:Suppress("UnstableApiUsage")


plugins {
    id("argo.plugin-conventions")
}

repositories {
    maven("https://packages.confluent.io/maven/")
}

dependencies {

    compileOnly(libs.schema.registry)

    testImplementation(libs.compiler)
    testImplementation(libs.schema.registry) {
        exclude("org.slf4j", "slf4j-reload4j")
            .because("SLF4J multiple binders warning.")
        exclude("org.slf4j", "slf4j-log4j12")
            .because("SLF4J multiple binders warning.")
    }

    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.strikt)
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
