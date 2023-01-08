@file:Suppress("DSL_SCOPE_VIOLATION")


plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.pluginPublish)
}

dependencies {

    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    implementation(gradleKotlinDsl())

    implementation(libs.compiler)

    testImplementation(libs.bundles.junit)
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_17.ordinal)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("-Xlint:-options")
    options.compilerArgs.add("-Werror")
}

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

gradlePlugin {
    plugins {
        create(property("ID").toString()) {
            id = property("ID").toString()
            implementationClass = property("IMPLEMENTATION_CLASS").toString()
            version = property("VERSION").toString()
            displayName = property("DISPLAY_NAME").toString()
        }
    }
}

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
