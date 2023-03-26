package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.columba.ColumbaConfigurer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion

@Suppress("unused")
abstract class ArgoPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        if (GradleVersion.current().baseVersion < GradleVersion.version("7.6")) {
            error("${ArgoPlugin::class.simpleName} needs a Gradle version '7.6' or higher.")
        }

        project.plugins.withType<JavaPlugin> {
            val extension = ArgoExtensionSupplier.get(project)
            project.logger.info("Using Gradle ${project.gradle.gradleVersion}.")

            /* Columba Setup */
            val columbaConfigurer = ColumbaConfigurer(extension.getColumba(), project)
            project.extensions.getByType<SourceSetContainer>()
                .configureEach { source ->
                    columbaConfigurer.configureFor(source)
                }
        }

    }
}
