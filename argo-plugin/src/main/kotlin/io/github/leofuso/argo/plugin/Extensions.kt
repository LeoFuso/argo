package io.github.leofuso.argo.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

fun SourceSet.configurationNameOf(base: String) = this.javaClass.getMethod("configurationNameOf", String::class.java)
    .let {
        it.isAccessible = true
        it.invoke(this, base) as String
    }

fun Project.addColumbaConfiguration(name: String, description: String, extension: ColumbaOptions) {
    this.configurations.create(name) { config ->
        config.description = description
        config.isVisible = false
        config.isTransitive = false
        config.isCanBeResolved = true
        config.isCanBeConsumed = false
        config.defaultDependencies { dependencySet ->

            val columba = dependencies.create(extension.getVersion().get())
            columba.because("A command line interface responsible for running the SpecificCompiler with process isolation.")
            dependencySet.add(columba)

            val compiler = dependencies.create(extension.getCompiler().get())
            compiler.because(
                "Compiler needed to generate code from Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) source files."
            )
            dependencySet.add(compiler)

            val jackson = dependencies.create(DEFAULT_JACKSON_DATABIND_DEPENDENCY)
            jackson.because("Fixes a vulnerability within 'org.apache.avro-compiler'.")
            dependencySet.add(jackson)

            logger.info("Using 'org.apache.avro-compiler' version: '{}' at {}.", compiler.version, name)
            logger.info("Using 'io.github.leofuso.columba:columba-cli' version: '{}' at {}.", columba.version, name)
        }
    }
}

fun Project.addCustomColumbaConfiguration(name: String, description: String) =
    configurations.maybeCreate(name).let { config ->
        config.isVisible = false
        config.isTransitive = true
        config.isCanBeResolved = true
        config.isCanBeConsumed = false
        config.description = description
    }

