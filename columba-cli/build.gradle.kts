@file:Suppress("UnstableApiUsage")

plugins {
    application
    id("argo.kotlin-conventions")
}

group = "io.github.leofuso.columba"
version = "0.1.2-SNAPSHOT"

val main: Configuration by configurations.creating {
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

application {
    mainClass.set("io.github.leofuso.columba.cli.MainKt")
}

dependencies {

    compileOnly(libs.compiler)
    compileOnly(libs.jacksonDatabind)
    compileOnly(libs.clikt)

    runtimeOnly(libs.slf4j)

    testRuntimeOnly(libs.junitLauncher)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.combinatorics)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.leofuso.columba.cli.MainKt"
    }
    group = "build"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

artifacts {
    add("main", tasks.jar)
}


/* Used as Debug artifact */
tasks.register<Jar>("uberJar") {

    manifest {
        attributes["Main-Class"] = "io.github.leofuso.columba.cli.MainKt"
    }

    group = "build"
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
