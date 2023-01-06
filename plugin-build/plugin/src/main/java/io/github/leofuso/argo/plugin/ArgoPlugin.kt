package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.options.OptionalGettersStrategy
import io.github.leofuso.argo.plugin.tasks.SpecificRecordCompileTask
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.generic.GenericData
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.idea.GenerateIdeaModule
import org.gradle.plugins.ide.idea.IdeaPlugin
import java.io.File

abstract class ArgoPlugin : Plugin<Project> {

    private lateinit var extension: ArgoExtension
    private lateinit var project: Project

    override fun apply(p: Project) {
        project = p
        extension = project.extensions.create("argo", ArgoExtension::class.java)
        addCompilerConfiguration()

        project.extensions.getByType(SourceSetContainer::class.java)
            .configureEach { source ->
                configureSpecificRecordCompileTask(source)
            }

        configureIdea()
    }

    private fun addCompilerConfiguration() {
        val extension = extension.getColumbae()
        extension.getCompilerVersion().convention("1.11.1")
        val config = project.configurations.create("compiler") {
            it.defaultDependencies { dependencies ->
                val version = extension.getCompilerVersion()
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

        val taskName = sourceSet
            .getTaskName("compile", "specificRecord")

        val extension = extension.getColumbae()
        val tasks = project.tasks

        val javaCompile = tasks.named(
            sourceSet.compileJavaTaskName,
            JavaCompile::class.java
        ).get()

        val register = tasks.register(taskName, SpecificRecordCompileTask::class.java) {
            sourceSet.java.srcDir(it.destination)

            it.source(project.avroSourceDir(sourceSet))
            it.include("**/*.${SCHEMA_EXTENSION}", "**/*.${PROTOCOL_EXTENSION}")

            it.destination.convention(project.layout.getSpecificRecordBuildDirectory(sourceSet))

            it.encoding.convention(javaCompile.options.encoding?: "UTF-8").set(extension.getOutputEncoding())
            it.additionalVelocityTools.convention(listOf())
            it.stringType.convention(GenericData.StringType.CharSequence)
            it.fieldVisibility.convention(SpecificCompiler.FieldVisibility.PRIVATE)
            it.useBigDecimal.convention(true)
            it.noSetters.convention(true)
            it.addExtraOptionalGetters.convention(false)
            it.optionalGetters.convention(OptionalGettersStrategy.ONLY_NULLABLE_FIELDS)
            it.logicalTypeFactories.convention(mapOf())
            it.additionalConverters.convention(listOf())
        }

        javaCompile.source(register)
        tasks.matching(sourceSet.sourcesJarTaskName::equals)
            .configureEach { it.dependsOn(register) }

        project.pluginManager.withPlugin(KOTLIN_PLUGIN_ID) {
            tasks.withType(SourceTask::class.java)
                .matching(sourceSet.getCompileTaskName(KOTLIN_LANGUAGE_NAME)::equals)
                .configureEach { it.source(register.get().destination) }
        }
    }

    private fun configureIdea() {

        project.plugins.withType(IdeaPlugin::class.java)
            .configureEach { ideaPlugin: IdeaPlugin ->

                val mainSourceSet = project.extensions.getByType(SourceSetContainer::class.java)
                    .getByName(SourceSet.MAIN_SOURCE_SET_NAME)

                val testSourceSet = project.extensions.getByType(SourceSetContainer::class.java)
                    .getByName(SourceSet.TEST_SOURCE_SET_NAME)

                val ideaModule = ideaPlugin.model.module

                ideaModule.sourceDirs = ideaModule.sourceDirs
                    .plus(project.avroSourceDir(mainSourceSet))
                    .plus(project.layout.getSpecificRecordBuildDirectory(mainSourceSet).map(Directory::getAsFile).get())

                ideaModule.testSources
                    .plus(project.avroSourceDir(testSourceSet))
                    .plus(project.layout.getSpecificRecordBuildDirectory(testSourceSet).map(Directory::getAsFile).get())

                ideaModule.excludeDirs = buildSet<File> {
                    addAll(ideaModule.excludeDirs)
                    remove(project.buildDir)

                    if (project.buildDir.isDirectory) {
                        project.buildDir.listFiles { f ->
                            f.isDirectory && (f.name.startsWith("generated-")).not()
                        }?.let { addAll(it) }
                    }
                }
            }

        project.tasks.withType(GenerateIdeaModule::class.java)
            .configureEach { module ->
                module.doFirst { task ->
                    project.tasks.withType(SpecificRecordCompileTask::class.java) {compileTask ->
                        project.mkdir(compileTask.destination.get())
                    }
                }
            }
    }
}
