package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.tasks.IDLProtocolTask
import io.github.leofuso.argo.plugin.tasks.SpecificRecordCompilerTask
import io.github.leofuso.argo.plugin.tasks.getSpecificRecordCompileBuildDirectory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
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

            addApacheAvroCompilerDependencyConfiguration(project, extension.getColumba())
            addCompileOnlyAvroIDLConfiguration(project)
            addCompileOnlySchemaProtocolConfiguration(project)

            project.extensions.getByType<SourceSetContainer>()
                .configureEach { source ->
                    configureColumbaTasks(project, extension.getColumba(), source)
                }
        }

    }

    private fun addApacheAvroCompilerDependencyConfiguration(project: Project, extension: ColumbaOptions) {

        val description = """
            |Compiler needed to generate code from Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) source files.
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

    private fun addCompileOnlyAvroIDLConfiguration(project: Project) {
        val description = """
            |CompileOnly dependencies containing IDL(.$IDL_EXTENSION) source files.
        """.trimMargin()

        val config = project.configurations.create("compileOnlyAvroIDL") {
            it.isVisible = true
            it.isCanBeResolved = true
            it.isCanBeConsumed = false
            it.description = description
        }
        project.configurations.findByName(COMPILE_ONLY_CONFIGURATION_NAME)?.let { config.extendsFrom(it) }
    }

    private fun addCompileOnlySchemaProtocolConfiguration(project: Project) {
        val description = """
            |CompileOnly dependencies containing Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) source files.
        """.trimMargin()

        val config = project.configurations.create("compileOnlySchemaProtocol") {
            it.isVisible = true
            it.isCanBeResolved = true
            it.isCanBeConsumed = false
            it.description = description
        }
        project.configurations.findByName(COMPILE_ONLY_CONFIGURATION_NAME)?.let { config.extendsFrom(it) }
    }

    private fun configureColumbaTasks(project: Project, extension: ColumbaOptions, sourceSet: SourceSet) {
        val taskContainer: TaskContainer = project.tasks

        val protocolTaskName = sourceSet.getTaskName("generate", "apacheAvroProtocol")
        val protocolTaskProvider = taskContainer.register<IDLProtocolTask>(protocolTaskName) {
            project.configurations.findByName("compileOnlyAvroIDL")?.let { configurableClasspath.from(it) }
            project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME).let { configurableClasspath.from(it) }
            configureSourceSet(sourceSet)
        }

        val javaTaskName = sourceSet.getCompileTaskName("apacheAvroJava")
        val javaTaskProvider: TaskProvider<SpecificRecordCompilerTask> =
            taskContainer.register<SpecificRecordCompilerTask>(javaTaskName) {
                project.configurations.findByName("compileOnlySchemaProtocol")?.let { source(it) }
                withExtension(extension)
                configureSourceSet(sourceSet)
                dependsOn(protocolTaskProvider)
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
