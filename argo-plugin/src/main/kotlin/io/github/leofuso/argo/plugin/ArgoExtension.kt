package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.columba.extensions.ColumbaOptions
import io.github.leofuso.argo.plugin.navis.extensions.NavisOptions
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke

object ArgoExtensionSupplier {
    fun get(project: Project): ArgoExtension {
        val extension = project.extensions.create<ArgoExtension>("argo")
        extension.getColumba().applyConventions()
        return extension
    }
}

@Suppress("unused")
abstract class ArgoExtension {

    @Nested
    abstract fun getColumba(): ColumbaOptions

    @Nested
    abstract fun getNavis(): NavisOptions

    fun columba(action: Action<in ColumbaOptions>) = action.invoke(getColumba())

    fun navis(action: Action<in NavisOptions>) = action.invoke(getNavis())
}
