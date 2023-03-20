package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.tasks.IDLProtocolTask
import io.github.leofuso.argo.plugin.tasks.SpecificRecordCompilerTask
import io.github.leofuso.argo.plugin.tasks.getSpecificRecordCompileBuildDirectory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.GenerateIdeaModule
import org.gradle.util.GradleVersion

@Suppress("unused")
abstract class ArgoPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        if (GradleVersion.current().baseVersion < GradleVersion.version("7.6")) {
            throw RuntimeException("${ArgoPlugin::class.simpleName} needs Gradle version 7.6 or higher.")
        }

        project.plugins.withType<JavaPlugin> {
            val extension = ArgoExtensionSupplier.get(project)
            project.logger.info("Using Gradle ${project.gradle.gradleVersion}.")

            /* Columba Setup */
            project.extensions.getByType<SourceSetContainer>()
                .configureEach { source ->
                    configureColumbaTasks(project, extension.getColumba(), source)
                }
        }

    }

    private fun configureColumbaTasks(project: Project, extension: ColumbaOptions, sourceSet: SourceSet) {

        /* Columba config */
        val columba = CONFIGURATION_COLUMBA + if (sourceSet.name == "main") "" else sourceSet.name.capitalized()
        project.addColumbaConfiguration(
            columba,
            "Needed dependencies to generate SpecificRecord Java source files in isolation.",
            extension
        )

        /* extra configs */
        val compileApacheAvroJavaConfiguration = project.addCompileApacheAvroJavaConfiguration(sourceSet)
        val specificCompilerTaskName = compileApacheAvroJavaConfiguration.name

        val specificCompilerSourcesName = "${specificCompilerTaskName}Sources"
        project.addCustomColumbaConfiguration(
            specificCompilerSourcesName,
            "Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) source files to be compiled."
        )

        val protocolTaskName = sourceSet.getTaskName("generate", "apacheAvroProtocol")
        project.addCustomColumbaConfiguration(
            protocolTaskName,
            "IDL(.$IDL_EXTENSION) source files needed for Protocol(.$PROTOCOL_EXTENSION) resolution."
        )

        val taskContainer: TaskContainer = project.tasks
        val protocolTaskProvider = taskContainer.register<IDLProtocolTask>(protocolTaskName) {
            configurableClasspath.from(
                project.configurations.getAt(columba),
                project.configurations.getAt(protocolTaskName)
            )
            configureAt(sourceSet)
        }

        val specificCompilerTaskProvider: TaskProvider<SpecificRecordCompilerTask> =
            taskContainer.register<SpecificRecordCompilerTask>(specificCompilerTaskName) {
                configurableClasspath.from(
                    project.configurations.getAt(columba),
                    project.configurations.getAt(specificCompilerTaskName)
                )
                source(
                    project.configurations.getAt(specificCompilerSourcesName)
                )
                withExtension(extension)
                configureAt(sourceSet)
                dependsOn(protocolTaskProvider)
            }

        /* Adding task dependency to JavaCompile task */
        taskContainer.named<JavaCompile>(sourceSet.compileJavaTaskName)
            .configure {
                it.source(specificCompilerTaskProvider)
                it.dependsOn(specificCompilerTaskProvider)
            }

        /* Adding task dependency to every task that generates a Jar */
        taskContainer.matching(sourceSet.sourcesJarTaskName::equals)
            .configureEach { it.dependsOn(specificCompilerTaskProvider) }

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
                it.dependsOn(specificCompilerTaskProvider)
            }

        /* Adding task dependency to Kotlin */
        project.pluginManager.withPlugin(KOTLIN_PLUGIN_ID) {
            taskContainer.withType<SourceTask>()
                .matching(sourceSet.getCompileTaskName(KOTLIN_LANGUAGE_NAME)::equals)
                .configureEach {
                    it.source(specificCompilerTaskProvider)
                    it.dependsOn(specificCompilerTaskProvider)
                }
        }
    }
}
