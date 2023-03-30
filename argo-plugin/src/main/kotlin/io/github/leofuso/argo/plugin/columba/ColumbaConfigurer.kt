package io.github.leofuso.argo.plugin.columba

import io.github.leofuso.argo.plugin.APACHE_AVRO_COMPILER_DEPENDENCY
import io.github.leofuso.argo.plugin.APACHE_COMMONS_TEXT_DEPENDENCY
import io.github.leofuso.argo.plugin.CLASS_EXTENSION
import io.github.leofuso.argo.plugin.COLUMBA_CLI_DEPENDENCY
import io.github.leofuso.argo.plugin.COLUMBA_CLI_DEPENDENCY_VERSION
import io.github.leofuso.argo.plugin.CONFIGURATION_COLUMBA
import io.github.leofuso.argo.plugin.IDL_EXTENSION
import io.github.leofuso.argo.plugin.JACKSON_DATABIND_DEPENDENCY
import io.github.leofuso.argo.plugin.KOTLIN_LANGUAGE_NAME
import io.github.leofuso.argo.plugin.KOTLIN_PLUGIN_ID
import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.columba.extensions.ColumbaOptions
import io.github.leofuso.argo.plugin.properties.GlobalProperties
import io.github.leofuso.argo.plugin.tasks.IDLProtocolTask
import io.github.leofuso.argo.plugin.tasks.SpecificRecordCompilerTask
import io.github.leofuso.argo.plugin.tasks.getSpecificRecordCompileBuildDirectory
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyConstraint
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.GenerateIdeaModule

class ColumbaConfigurer(private val extension: ColumbaOptions, private val project: Project) {

    fun configureFor(sourceSet: SourceSet) {

        /* Default config */
        val columbaConfiguration = project.addColumbaConfiguration(sourceSet, extension)

        /* Compile config */
        val compileApacheAvroJavaConfiguration = project.addCompileApacheAvroJavaConfiguration(sourceSet)

        /* Source configs */
        val specificCompilerSourcesName = "${compileApacheAvroJavaConfiguration.name}Sources"
        val compileApacheAvroJavaSourcesConfiguration = project.addSourcesConfiguration(
            specificCompilerSourcesName,
            "Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) source files to be compiled."
        )

        /* Tasks configs */
        val generateApacheAvroProtocolTaskName = sourceSet.getTaskName("generate", "apacheAvroProtocol")
        val generateApacheAvroProtocolConfiguration = project.addSourcesConfiguration(
            generateApacheAvroProtocolTaskName,
            "IDL(.$IDL_EXTENSION) source files needed for Protocol(.$PROTOCOL_EXTENSION) resolution."
        )

        val taskContainer: TaskContainer = project.tasks
        val generateApacheAvroProtocol =
            taskContainer.register<IDLProtocolTask>(generateApacheAvroProtocolConfiguration.name) {
                configurableClasspath.from(columbaConfiguration, generateApacheAvroProtocolConfiguration)
                launcher.set(extension.getLauncher())
                configureAt(sourceSet)
            }

        val compileApacheAvroJava: TaskProvider<SpecificRecordCompilerTask> =
            taskContainer.register<SpecificRecordCompilerTask>(compileApacheAvroJavaConfiguration.name) {
                configurableClasspath.from(columbaConfiguration, compileApacheAvroJavaConfiguration)
                source(compileApacheAvroJavaSourcesConfiguration)
                withExtension(extension)
                launcher.set(extension.getLauncher())
                configureAt(sourceSet)
                dependsOn(generateApacheAvroProtocol)
            }

        /* Adding task dependency to JavaCompile task */
        taskContainer.named<JavaCompile>(sourceSet.compileJavaTaskName)
            .configure {
                it.source(compileApacheAvroJava)
                it.dependsOn(compileApacheAvroJava)
            }

        /* Adding task dependency to every task that generates a Jar */
        taskContainer.matching(sourceSet.sourcesJarTaskName::equals)
            .configureEach { it.dependsOn(compileApacheAvroJava) }

        /*
         * Adding a task dependency to Idea module,
         * [.ref](https://discuss.gradle.org/t/how-do-i-get-intellij-to-recognize-gradle-generated-sources-dir/16847/5)
         */
        taskContainer.withType<GenerateIdeaModule>()
            .configureEach {
                val buildDirectory = getSpecificRecordCompileBuildDirectory(project, sourceSet)
                val buildDirectoryFile = buildDirectory.get().asFile
                project.mkdir(buildDirectoryFile)
                it.module.generatedSourceDirs.plusAssign(buildDirectoryFile)
                it.dependsOn(compileApacheAvroJava)
            }

        /* Adding task dependency to Kotlin */
        project.pluginManager.withPlugin(KOTLIN_PLUGIN_ID) {
            taskContainer.withType<SourceTask>()
                .matching(sourceSet.getCompileTaskName(KOTLIN_LANGUAGE_NAME)::equals)
                .configureEach {
                    it.source(compileApacheAvroJava)
                    it.dependsOn(compileApacheAvroJava)
                }
        }
    }

    private fun SourceSet.getColumbaConfigurationName(): String = CONFIGURATION_COLUMBA + if (name == "main") "" else name.capitalized()

    @Suppress("UnstableApiUsage")
    private fun Project.addColumbaConfiguration(sourceSet: SourceSet, extension: ColumbaOptions): Configuration =
        configurations.create(sourceSet.getColumbaConfigurationName()) { config ->

            config.description = "Needed dependencies to generate 'SpecificRecord' Java source files in isolation."
            config.isVisible = false
            config.isTransitive = true
            config.isCanBeResolved = true
            config.isCanBeConsumed = false

            afterEvaluate {
                dependencies {
                    val properties = GlobalProperties
                    val dependencyNotation = properties.getProperty(COLUMBA_CLI_DEPENDENCY)
                    val columba = dependencies.create(dependencyNotation) {
                        because("A command line interface responsible for running the SpecificCompiler with process isolation.")
                        version { versionConstraint ->
                            val version = extension.getVersion()
                            if (version.isPresent) {
                                versionConstraint.strictly(version.get())
                            } else {
                                val versionNotation = properties.getProperty(COLUMBA_CLI_DEPENDENCY_VERSION)
                                versionConstraint.prefer(versionNotation)
                            }
                        }
                    }
                    add(config.name, columba)
                }

                dependencies.constraints { handler ->

                    val compiler = handler.addConstraint(
                        APACHE_AVRO_COMPILER_DEPENDENCY,
                        "Compiler needed to generate code from Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) source files.",
                        extension.getCompilerVersion().orNull
                    )
                    handler.add(config.name, compiler)

                    val jackson = handler.addConstraint(
                        JACKSON_DATABIND_DEPENDENCY,
                        "Fixes a vulnerability within 'org.apache.avro:avro-compiler:${compiler.version}'."
                    )
                    handler.add(config.name, jackson)

                    val commons = handler.addConstraint(
                        APACHE_COMMONS_TEXT_DEPENDENCY,
                        "Fixes 'CVE-2022-42889' vulnerability within 'org.apache.avro:avro-compiler:${compiler.version}'."
                    )
                    handler.add(config.name, commons)
                }
            }
        }

    private fun DependencyConstraintHandler.addConstraint(key: String, cause: String, strictly: String? = null): DependencyConstraint {
        val properties = GlobalProperties
        val notation = properties.getProperty(key)
        return create(notation) { constraint ->
            constraint.because(cause)
            constraint.version { versionConstraint ->
                if (strictly != null) {
                    versionConstraint.strictly(strictly)
                }
                val preferred = properties.getProperty("$key.version")
                versionConstraint.prefer(preferred)
            }
        }
    }

    private fun Project.addSourcesConfiguration(name: String, description: String): Configuration = configurations.maybeCreate(name)
        .let { config ->
            config.isVisible = false
            config.isTransitive = false
            config.isCanBeResolved = true
            config.isCanBeConsumed = false
            config.description = description
            config
        }

    private fun Project.addCompileApacheAvroJavaConfiguration(sourceSet: SourceSet): Configuration {
        val name = sourceSet.getCompileTaskName("apacheAvroJava")
        return project.configurations.maybeCreate(name)
            .let { config ->
                config.isVisible = false
                config.isTransitive = true
                config.isCanBeResolved = true
                config.isCanBeConsumed = false
                config.description = "Additional Classes(.$CLASS_EXTENSION) needed for 'SpecificRecord' Java source file generation."

                project.afterEvaluate {
                    it.configurations
                        .getAt(sourceSet.implementationConfigurationName)
                        .extendsFrom(config)
                }
                config
            }
    }

}
