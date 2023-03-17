@file:Suppress("UnstableApiUsage")

plugins {
    id("argo.plugin-conventions")
}

group = ARGO_GROUP
version = Versions.ARGO

dependencies {
    implementation(libs.compiler)
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
            id = ARGO_GROUP
            implementationClass = "$ARGO_GROUP.plugin.ArgoPlugin"
            displayName = "Argo"
            description = """
                A Gradle plugin aimed to help working with Apache Avro.
                It supports code generation for JSON schema declaration files(.avsc),
                JSON protocol declaration files(.avpr), and Avro IDL files.
                In the future, it should support Schema Registry integration, as well.
            """
            tags.set(listOf("avro", "kafka", "schema-registry", "confluent", "java", "code generation"))
        }
    }
}
