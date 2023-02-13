package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.tasks.IDLProtocolTask
import io.github.leofuso.argo.plugin.tasks.SpecificRecordCompilerTask
import io.github.leofuso.argo.plugin.tasks.getSpecificRecordCompileBuildDirectory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.GenerateIdeaModule

abstract class ArgoPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = ArgoExtensionSupplier.get(project)

        /* Columba setup */
        project.plugins.withType<JavaPlugin> {
            project.extensions.getByType<SourceSetContainer>()
                .configureEach { source ->
                    configureColumbaTasks(project, extension.getColumba(), source)
                }
            addApacheAvroCompilerDependency(project, extension.getColumba())
        }

    }

    private fun addApacheAvroCompilerDependency(project: Project, extension: ColumbaOptions) {
        val description = """
            |Compiler needed to generate code from Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) files.
        """.trimMargin()

        val config = project.configurations.create("apacheAvroCompiler") {
            it.isVisible = true
            it.isCanBeResolved = false
            it.isCanBeConsumed = true
            it.description = description

            it.defaultDependencies { dependencies ->
                val compilerDependency = project.dependencies.create(extension.getCompiler().get())
                project.logger.lifecycle("Using 'org.apache.avro-compiler' version: '{}'.", compilerDependency.version)
                val jacksonDependency = project.dependencies.create(DEFAULT_JACKSON_DATABIND_DEPENDENCY)
                dependencies.add(compilerDependency)
                dependencies.add(jacksonDependency)
            }
        }
        project.configurations.findByName(COMPILE_CLASSPATH_CONFIGURATION_NAME)?.extendsFrom(config)
    }

    private fun configureColumbaTasks(project: Project, extension: ColumbaOptions, sourceSet: SourceSet) {
        val taskContainer: TaskContainer = project.tasks

        val protocolTaskName = sourceSet.getTaskName("generate", "apacheAvroProtocol")
        taskContainer.register<IDLProtocolTask>(protocolTaskName) {
            configureSourceSet(sourceSet)
            classpath = project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        }

        val javaTaskName = sourceSet.getCompileTaskName("apacheAvroJava")
        val javaTaskProvider: TaskProvider<SpecificRecordCompilerTask> =
            taskContainer.register<SpecificRecordCompilerTask>(javaTaskName) {
                withExtension(extension)
                configureSourceSet(sourceSet)
                val protocolTask = project.tasks.findByName(protocolTaskName)
                getSources().builtBy(protocolTask)
            }

        /* Adding task dependency to JavaCompile task */
        taskContainer.named<JavaCompile>(sourceSet.compileJavaTaskName)
            .configure {
                it.source(javaTaskProvider)
                it.dependsOn(javaTaskProvider)
            }

        /* Adding task dependency to every task that generates a Jar */
        taskContainer.matching(sourceSet.sourcesJarTaskName::equals)
            .configureEach { it.dependsOn(javaTaskProvider) }

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
                it.dependsOn(javaTaskProvider)
            }

        /* Adding task dependency to Kotlin */
        project.pluginManager.withPlugin(KOTLIN_PLUGIN_ID) {
            taskContainer.withType<SourceTask>()
                .matching(sourceSet.getCompileTaskName(KOTLIN_LANGUAGE_NAME)::equals)
                .configureEach {
                    it.source(javaTaskProvider)
                    it.dependsOn(javaTaskProvider)
                }
        }
    }
}
