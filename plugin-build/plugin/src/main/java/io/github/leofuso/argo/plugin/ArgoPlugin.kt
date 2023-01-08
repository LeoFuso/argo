package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.OptionalGettersStrategy.ONLY_NULLABLE_FIELDS
import io.github.leofuso.argo.plugin.tasks.SpecificRecordCompilerTask
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.generic.GenericData
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.idea.GenerateIdeaModule
import org.gradle.plugins.ide.idea.IdeaPlugin
import java.io.File

import org.gradle.kotlin.dsl.*

abstract class ArgoPlugin : Plugin<Project> {

    private lateinit var extension: ArgoExtension
    private lateinit var project: Project

    override fun apply(p: Project) {
        project = p
        project.plugins.apply(JavaPlugin::class.java)
        extension = project.extensions.create("argo")
        addCompilerConfiguration()

        project.extensions.getByType(SourceSetContainer::class.java)
            .configureEach(::configureSpecificRecordCompileTask)

        configureIdea()
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

        val taskName = sourceSet
            .getTaskName("compile", "specificRecord")

        val extension = extension.getColumba()
        val tasks = project.tasks

        val javaCompile = tasks.named(
            sourceSet.compileJavaTaskName,
            JavaCompile::class.java
        ).get()

        val register = tasks.register(taskName, SpecificRecordCompilerTask::class.java) {

            sourceSet.java.srcDir(it.getOutputDir())
            it.source(project.avroSourceDir(sourceSet))
            it.include("**/*.${SCHEMA_EXTENSION}", "**/*.${PROTOCOL_EXTENSION}")
            extension.getExcluded().orNull?.let { excluded -> it.exclude(excluded) }

            val fields = extension.getFields()
            it.getStringType().convention(GenericData.StringType.CharSequence).set(fields.getStringType())
            it.getFieldVisibility().convention(SpecificCompiler.FieldVisibility.PRIVATE).set(fields.getVisibility())
            it.useDecimalType.convention(true).set(fields.useDecimalTypeProvider)

            val accessors = extension.getAccessors()
            it.noSetters.convention(true).set(accessors.noSetterProvider)
            it.addExtraOptionalGetters.convention(false).set(accessors.addExtraOptionalGettersProvider)
            it.useOptionalGetters.convention(true).set(accessors.useOptionalGettersProvider)
            it.getOptionalGettersStrategy().convention(ONLY_NULLABLE_FIELDS).set(accessors.getOptionalGettersStrategy())

            it.getOutputDir().convention(project.layout.getSpecificRecordBuildDirectory(sourceSet))
            it.getEncoding().convention(javaCompile.options.encoding ?: "UTF-8").set(extension.getOutputEncoding())
            it.getAdditionalVelocityTools().convention(listOf())
                .set(extension.getAdditionalVelocityTools())

            it.getAdditionalLogicalTypeFactories().convention(listOf())
                .set(extension.getAdditionalLogicalTypeFactories())

            it.getAdditionalConverters().convention(listOf())
                .set(extension.getAdditionalConverters())

            it.getVelocityTemplateDirectory()
                .set(extension.getVelocityTemplateDirectory())
        }

        javaCompile.source(register)
        tasks.matching(sourceSet.sourcesJarTaskName::equals)
            .configureEach { it.dependsOn(register) }

        project.pluginManager.withPlugin(KOTLIN_PLUGIN_ID) {
            tasks.withType(SourceTask::class.java)
                .matching(sourceSet.getCompileTaskName(KOTLIN_LANGUAGE_NAME)::equals)
                .configureEach { it.source(register.get().getOutputDir()) }
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
                module.doFirst {
                    project.tasks.withType(SpecificRecordCompilerTask::class.java) { compileTask ->
                        project.mkdir(compileTask.getOutputDir().get())
                    }
                }
            }
    }
}
