@file:Suppress("UnstableApiUsage")

plugins {
    id("argo.plugin-conventions")
}

group = ARGO_GROUP
version = Versions.ARGO

dependencies {
    implementation(libs.compiler)
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")
    implementation("org.apache.logging.log4j:log4j-iostreams:2.20.0")
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
