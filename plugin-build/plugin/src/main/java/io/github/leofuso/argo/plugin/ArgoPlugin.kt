package io.github.leofuso.argo.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.plugins.ide.idea.IdeaPlugin

abstract class ArgoPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val extension = project.extensions.create("argo", ArgoExtension::class.java)


        project.tasks.register("generateAvro", TemplateExampleTask::class.java) {
//            it.tag.set(extension.tag)
//            it.message.set(extension.message)
//            it.outputFile.set(extension.outputFile)
        }
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
