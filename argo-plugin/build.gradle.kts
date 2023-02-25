@file:Suppress("UnstableApiUsage")

plugins {
    id("argo.plugin-conventions")
}

group = "io.github.leofuso.argo"
version = "0.1.0"

dependencies {
    implementation(libs.compiler)
    implementation(libs.jacksonDatabind)
    testRuntimeOnly(libs.junitLauncher)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.combinatorics)
}

gradlePlugin {
    website.set("https://github.com/LeoFuso/argo")
    vcsUrl.set("https://github.com/LeoFuso/argo")
    plugins {
        create("argoPlugin") {
            id = "io.github.leofuso.argo"
            implementationClass = "io.github.leofuso.argo.plugin.ArgoPlugin"
            displayName = "Argo"
            description = """
                |A Gradle plugin aimed to help working with Apache Avro.
                |It supports code generation for JSON schema declaration files(.avsc),
                |JSON protocol declaration files(.avpr), and Avro IDL files.
                |In the future, it should support Schema Registry integration, as well."
            """.trimMargin().replace("\n", "")
            tags.set(listOf("plugin", "gradle", "avro", "kafka", "schema-registry", "confluent", "java"))
        }
    }
}
