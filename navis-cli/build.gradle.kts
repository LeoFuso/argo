import Versions.replace

plugins {
    application
    id("argo.kotlin-conventions")
    id("argo.artifact-conventions")
}

application {
    mainClass.set("io.github.leofuso.navis.cli.MainKt")
}

repositories {
    maven("https://packages.confluent.io/maven/")
}

configurations.all {
    replace(libs.compiler) { "Conflict resolution." }
    replace(libs.jackson.databind) { "The version of the compiler has a security issue associated with this dependency." }
    replace(libs.gson) { "The version of the compiler has a security issue associated with this dependency." }
    replace(libs.apache.commons.text) { "The version of the compiler has a security issue associated with this dependency." }
    replace(libs.apache.commons.lang) { "Conflict resolution." }
    replace(libs.slf4j.api) { "Conflict resolution." }
}

val internal: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, JavaVersion.current().majorVersion.toInt())
        attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named(GradlePluginApiVersion::class.java, GradleVersion.current().version))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named( "${project.group}:${project.name}:${project.version}"))
    }
}

dependencies {


    api(libs.clikt) { because("Facilitate CLI implementation.") }
    implementation(libs.compiler) { because("Decoupling the runtime environment. A user can choose the compiler version.") }
    implementation(libs.schema.registry) {
        exclude("org.slf4j", "slf4j-reload4j")
            .because("SLF4J multiple binders warning.")
        exclude("org.slf4j", "slf4j-log4j12")
            .because("SLF4J multiple binders warning.")
    }

    runtimeOnly(libs.slf4j.simple) { because("libs.compiler depends on SLF4J.") }

    testRuntimeOnly(libs.junit.launcher)
    testRuntimeOnly(libs.compiler)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.combinatorics)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.leofuso.navis.cli.MainKt"
    }
    group = "build"
}

artifacts {
    add("internal", tasks.jar)
}
