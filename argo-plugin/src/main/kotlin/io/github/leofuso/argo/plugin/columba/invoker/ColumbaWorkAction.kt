package io.github.leofuso.argo.plugin.columba.invoker

import io.github.leofuso.argo.plugin.columba.invoker.ColumbaWorkAction.ColumbaWorkParameters
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.lang.reflect.InvocationTargetException

abstract class ColumbaWorkAction : WorkAction<ColumbaWorkParameters> {

    private fun buildInvoker() = if (parameters.noop.isPresent && parameters.noop.get()) {
        NoopColumbaInvoker(parameters.classpath)
    } else {
        DefaultColumbaInvoker()
    }

    override fun execute() {

        try {

            buildInvoker()
                .invoke(parameters.arguments.get())

        } catch (ex: InvocationTargetException) {
            val message = ex.targetException.message
            if (message != null) {
                throw GradleException(message, ex)
            }
            throw GradleException("There was a problem running Columba.", ex)
        }
    }

    interface ColumbaWorkParameters : WorkParameters {
        val arguments: ListProperty<String>
        val noop: Property<Boolean>
        val classpath: ConfigurableFileCollection
    }

}
