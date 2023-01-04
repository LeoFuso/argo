package io.github.leofuso.argo.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.util.internal.VersionNumber

abstract class ArgoPlugin : Plugin<Project> {

    private lateinit var extension: ArgoExtension

    override fun apply(project: Project) {

        extension = project.extensions.create("argo", ArgoExtension::class.java)
        addAvroCompilerConfiguration(project, extension)

        project.tasks.register("generateAvro", TemplateExampleTask::class.java) {
//            it.tag.set(extension.tag)
//            it.message.set(extension.message)
//            it.outputFile.set(extension.outputFile)
        }
    }

    private fun addAvroCompilerConfiguration(project: Project, extension: ArgoExtension) {
        val config = project.configurations.create("compiler") {
            it.defaultDependencies { dependencies ->
                val versionProperty = extension.getColumbae().version
                versionProperty.convention(AVRO_COMPILER_DEFAULT_RAW_VERSION)
                val version = VersionNumber.parse(versionProperty.get())
                val compilerDependency = String.format(AVRO_COMPILER_TEMPLATE_DEPENDENCY, version)
                dependencies.add(project.dependencies.create(compilerDependency))
            }
        }
        config.isVisible = true
        config.isCanBeResolved = false
        config.isCanBeConsumed = true
        config.description = "Compiler needed to generate code from .avsc and .avpr files."
    }
    private fun configureIntelliJ(project: Project) {
        project.plugins.withType(IdeaPlugin::class.java)
            .configureEach { ideaPlugin: IdeaPlugin ->
                val ideaModule = ideaPlugin.model.module
                ideaModule.generatedSourceDirs.plus(
                    project.layout.buildDirectory
                        .dir("/generated-sources-avro-java")
                        .map(Directory::getAsFile)
                )
            }
    }
}
