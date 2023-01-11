package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.tasks.SpecificRecordCompilerTask
import io.github.leofuso.argo.plugin.tasks.getSpecificRecordCompileBuildDirectory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.GenerateIdeaModule

abstract class ArgoPlugin : Plugin<Project> {

    private lateinit var extension: ArgoExtension
    private lateinit var project: Project

    override fun apply(projectArg: Project) {
        project = projectArg
        extension = project.extensions.create("argo")

        project.plugins.withType<JavaPlugin> {
            project.extensions.getByType<SourceSetContainer>()
                .configureEach(::configureSpecificRecordCompileTask)
        }

        // addCompilerConfiguration()
    }

    private fun addCompilerConfiguration() {
        val extension = extension.getColumba()
        //extension.getCompilerVersion().convention("1.11.1")
        val config = project.configurations.create("compiler") {
            it.defaultDependencies { dependencies ->
                val version = "1.11.1"
                val compilerDependency = String.format("org.apache.avro:avro-compiler:%s", version)
                dependencies.add(project.dependencies.create(compilerDependency))
            }
        }
        config.isVisible = true
        config.isCanBeResolved = false
        config.isCanBeConsumed = true
        config.description = "Compiler needed to generate code from .avsc and .avpr files."
    }

    private fun configureSpecificRecordCompileTask(sourceSet: SourceSet) {

        val taskContainer: TaskContainer = project.tasks

        val taskName = sourceSet.getCompileTaskName("apacheAvroJava")
        val taskProvider: TaskProvider<SpecificRecordCompilerTask> =
            taskContainer.register<SpecificRecordCompilerTask>(taskName) {
                val extension: ColumbaOptions = extension.getColumba().withConventions(project)
                withExtension(extension)
                configureSourceSet(sourceSet)
            }

        /* Adding task dependency to JavaCompile task */
        taskContainer.named<JavaCompile>(sourceSet.compileJavaTaskName)
            .configure {
                it.source(taskProvider)
                it.dependsOn(taskProvider)
            }

        /* Adding task dependency to every task that generates a Jar */
        taskContainer.matching(sourceSet.sourcesJarTaskName::equals)
            .configureEach { it.dependsOn(taskProvider) }

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
                it.dependsOn(taskProvider)
            }

        /* Adding task dependency to Kotlin */
        project.pluginManager.withPlugin(KOTLIN_PLUGIN_ID) {
            taskContainer.withType<SourceTask>()
                .matching(sourceSet.getCompileTaskName(KOTLIN_LANGUAGE_NAME)::equals)
                .configureEach {
                    it.source(taskProvider)
                    it.dependsOn(taskProvider)
                }
        }
    }
}
