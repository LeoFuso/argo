@file:Suppress("UnstableApiUsage")

package io.github.leofuso.argo.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies

fun SourceSet.getColumbaConfigurationName() = CONFIGURATION_COLUMBA + if (name == "main") "" else name.capitalized()

fun Project.addColumbaConfiguration(sourceSet: SourceSet, extension: ColumbaOptions) =
    configurations.create(sourceSet.getColumbaConfigurationName()) { config ->
        config.description = "Needed dependencies to generate SpecificRecord Java source files in isolation."
        config.isVisible = false
        config.isTransitive = true
        config.isCanBeResolved = true
        config.isCanBeConsumed = false

        afterEvaluate {

            dependencies {

                val columba = dependencies.create(DEFAULT_COLUMBA_CLI_DEPENDENCY) {
                    because("A command line interface responsible for running the SpecificCompiler with process isolation.")
                    version {
                        val version = extension.getVersion()
                        if (version.isPresent) {
                            it.strictly(version.get())
                        } else {
                            it.prefer(DEFAULT_COLUMBA_CLI_DEPENDENCY_VERSION)
                        }
                    }
                }

                add(config.name, columba)
            }

            dependencies.constraints { handler ->

                val compiler = handler.create(DEFAULT_APACHE_AVRO_COMPILER_DEPENDENCY) { constraint ->
                    constraint.because(
                        "Compiler needed to generate code from Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) source files."
                    )
                    constraint.version {
                        val version = extension.getCompilerVersion()
                        if (version.isPresent) {
                            it.strictly(version.get())
                        } else {
                            it.prefer(DEFAULT_APACHE_AVRO_COMPILER_DEPENDENCY_VERSION)
                        }
                    }
                }
                handler.add(config.name, compiler)

                val jackson = handler.create(DEFAULT_JACKSON_DATABIND_DEPENDENCY) { constraint ->
                    constraint.because("Fixes a vulnerability within 'org.apache.avro:avro-compiler:${compiler.version}'.")
                    constraint.version {
                        it.prefer(DEFAULT_JACKSON_DATABIND_DEPENDENCY_VERSION)
                    }
                }
                handler.add(config.name, jackson)
            }
        }
    }

fun Project.addSourcesConfiguration(name: String, description: String) = configurations.maybeCreate(name)
    .let { config ->
        config.isVisible = false
        config.isTransitive = false
        config.isCanBeResolved = true
        config.isCanBeConsumed = false
        config.description = description
        config
    }

fun Project.addCompileApacheAvroJavaConfiguration(sourceSet: SourceSet) =
    configurations.maybeCreate(sourceSet.getCompileTaskName("apacheAvroJava"))
        .let { config ->
            config.isVisible = false
            config.isTransitive = true
            config.isCanBeResolved = true
            config.isCanBeConsumed = false
            config.description = "Additional Classes(.$CLASS_EXTENSION) needed for SpecificRecord Java source file generation."

            afterEvaluate {
                configurations
                    .getAt(sourceSet.implementationConfigurationName)
                    .extendsFrom(config)
            }

            config
        }
