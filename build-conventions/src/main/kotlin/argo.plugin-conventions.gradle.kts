@file:Suppress("UnstableApiUsage")



plugins {
    id("argo.kotlin-conventions")
    id("argo.signing-conventions")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    testImplementation(gradleTestKit())
}

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks {

    val buildVersionProperties by registering(WriteProperties::class) {

        group = "build"
        description = "Build 'versions.properties' file with the default dependencies versions to be used by the plugin."
        encoding = "UTF-8"
        outputFile = file("$buildDir/versions.properties")

        val versionCatalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

        val columba = versionCatalog.findLibrary("columba-cli").get().get()
        property("libs.columba.cli", columba.module)
        property("libs.columba.cli.version", columba.versionConstraint.toString())

        val compiler = versionCatalog.findLibrary("compiler").get().get()
        property("libs.compiler", compiler.module)
        property("libs.compiler.version", compiler.versionConstraint.toString())

        val jacksonDatabind = versionCatalog.findLibrary("jackson-databind").get().get()
        property("libs.jackson.databind", jacksonDatabind.module)
        property("libs.jackson.databind.version", jacksonDatabind.versionConstraint.toString())

        val apacheCommonsText = versionCatalog.findLibrary("apache-commons-text").get().get()
        property("libs.apache.commons.text", apacheCommonsText.module)
        property("libs.apache.commons.text.version", apacheCommonsText.versionConstraint.toString())
    }

    processResources {
        from(buildVersionProperties)
    }

    processTestResources {
        from(buildVersionProperties)
    }
}
