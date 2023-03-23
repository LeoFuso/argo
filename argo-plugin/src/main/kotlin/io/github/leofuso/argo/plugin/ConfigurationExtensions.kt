@file:Suppress("UnstableApiUsage")

package io.github.leofuso.argo.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import java.util.*

fun SourceSet.getColumbaConfigurationName() = CONFIGURATION_COLUMBA + if (name == "main") "" else name.capitalized()

fun Project.addColumbaConfiguration(sourceSet: SourceSet, extension: ColumbaOptions) =
    configurations.create(sourceSet.getColumbaConfigurationName()) { config ->
        config.description = "Needed dependencies to generate SpecificRecord Java source files in isolation."
        config.isVisible = false
        config.isTransitive = true
        config.isCanBeResolved = true
        config.isCanBeConsumed = false

        afterEvaluate {

            val properties = Properties()
            val resource = ArgoPlugin::class.java.classLoader.getResource("versions.properties")
                ?: error("Could not resolve 'versions.properties'.")

            properties.load(resource.openStream())

            dependencies {
                val dependencyNotation = properties.getProperty(COLUMBA_CLI_DEPENDENCY)
                val columba = dependencies.create(dependencyNotation) {
                    because("A command line interface responsible for running the SpecificCompiler with process isolation.")
                    version {
                        val version = extension.getVersion()
                        if (version.isPresent) {
                            it.strictly(version.get())
                        } else {
                            val versionNotation = properties.getProperty(COLUMBA_CLI_DEPENDENCY_VERSION)
                            it.prefer(versionNotation)
                        }
                    }
                }
                add(config.name, columba)
            }

            dependencies.constraints { handler ->

                val compiler = handler.create(properties.getProperty(APACHE_AVRO_COMPILER_DEPENDENCY)) { constraint ->
                    constraint.because(
                        "Compiler needed to generate code from Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) source files."
                    )
                    constraint.version {
                        val version = extension.getCompilerVersion()
                        if (version.isPresent) {
                            it.strictly(version.get())
                        } else {
                            it.prefer(properties.getProperty(APACHE_AVRO_COMPILER_DEPENDENCY_VERSION))
                        }
                    }
                }
                handler.add(config.name, compiler)

                val jackson = handler.create(properties.getProperty(JACKSON_DATABIND_DEPENDENCY)) { constraint ->
                    constraint.because("Fixes a vulnerability within 'org.apache.avro:avro-compiler:${compiler.version}'.")
                    constraint.version {
                        it.prefer(properties.getProperty(JACKSON_DATABIND_DEPENDENCY_VERSION))
                    }
                }
                handler.add(config.name, jackson)

                val commons = handler.create(properties.getProperty(APACHE_COMMONS_TEXT_DEPENDENCY)) { constraint ->
                    constraint.because("Fixes 'CVE-2022-42889' vulnerability within 'org.apache.avro:avro-compiler:${compiler.version}'.")
                    constraint.version {
                        it.prefer(properties.getProperty(APACHE_COMMONS_TEXT_DEPENDENCY_VERSION))
                    }
                }
                handler.add(config.name, commons)
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
