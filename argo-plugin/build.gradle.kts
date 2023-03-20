@file:Suppress("UnstableApiUsage")

plugins {
    id("argo.plugin-conventions")
}

group = "io.github.leofuso.argo"
version = System.getProperty("global.version")

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
            id = "io.github.leofuso.argo"
            implementationClass = "io.github.leofuso.argo.plugin.ArgoPlugin"
            displayName = "Argo"
            description = """
                A Gradle plugin that supports Java code generation from JSON schema declaration files(.avsc),
                JSON protocol declaration files(.avpr), and Avro IDL(.avdl) source files
            """.trimIndent()
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
