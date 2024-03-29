@file:Suppress("UnstableApiUsage")

import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE
import org.gradle.api.attributes.plugin.GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE

plugins {
    application
    id("argo.kotlin-conventions")
    id("argo.artifact-conventions")
}

application {
    mainClass.set("io.github.leofuso.columba.cli.MainKt")
}

val internal: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
        attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
        attribute(TARGET_JVM_VERSION_ATTRIBUTE, JavaVersion.current().majorVersion.toInt())
        attribute(GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named(GradlePluginApiVersion::class.java, GradleVersion.current().version))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named( "${project.group}:${project.name}:${project.version}"))
    }
}

dependencies {

    api(libs.clikt) { because("Facilitate CLI implementation.") }
    implementation(libs.compiler) { because("Decoupling the runtime environment. A user can choose the compiler version.") }
    implementation(libs.jackson.databind) { because("The version of the compiler has a security issue associated with this dependency.") }
    implementation(libs.apache.commons.text) { because("The version of the compiler has a security issue associated with this dependency.") }

    runtimeOnly(libs.slf4j.simple) { because("libs.compiler depends on SLF4J.") }

    testRuntimeOnly(libs.junit.launcher)
    testRuntimeOnly(libs.compiler)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.combinatorics)

}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.leofuso.columba.cli.MainKt"
    }
    group = "build"
}

tasks.register("buildCacheDir") {
    group = "build setup"
    doLast {
        val cache = File(project.projectDir.parentFile, ".gradle${File.separator}cache")
        if(!cache.exists()) {
            cache.mkdirs()
        }

        copy {
            from(configurations.compileClasspath, tasks.jar)
            into("$cache${File.separator}libs")
        }
    }
}

tasks.register<Jar>("uberJar") {

    manifest {
        attributes["Main-Class"] = "io.github.leofuso.columba.cli.MainKt"
    }

    group = "build"
    description = "Generates a .jar containing all needed dependencies."
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveClassifier.set("uber")
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}

artifacts {
    add("internal", tasks.jar)
    tasks.findByName("uberJar")?.let { task ->
        add("internal", task)
    }
}
