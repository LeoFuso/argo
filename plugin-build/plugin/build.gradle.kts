
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
}

dependencies {

    implementation(kotlin("stdlib"))
    implementation(gradleApi())

    testImplementation(libs.junit)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create("io.github.leofuso.argo") {
            id = "io.github.leofuso.argo"
            implementationClass = "io.github.leofuso.argo.plugin.ArgoPlugin"
            version = "0.0.1-SNAPSHOT"
            displayName = "Argo"
        }
    }
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
    website = property("WEBSITE").toString()
    vcsUrl = property("VCS_URL").toString()
    description = property("DESCRIPTION").toString()
    tags = listOf("plugin", "gradle", "avro", "kafka", "schema-registry", "confluent", "java")
}

tasks.create("setupPluginUploadFromEnvironment") {
    doLast {

        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}
