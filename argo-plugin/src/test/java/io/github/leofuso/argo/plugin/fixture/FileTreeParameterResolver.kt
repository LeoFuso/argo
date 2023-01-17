package io.github.leofuso.argo.plugin.fixture

import io.github.leofuso.argo.plugin.fixture.annotation.SchemaParameter
import org.gradle.api.file.FileTree
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class FileTreeParameterResolver : ParameterResolver {

    override fun supportsParameter(parameter: ParameterContext, extension: ExtensionContext): Boolean {
        val subject = parameter.parameter
        val parameterType = subject.type
        return FileTree::class.java.isAssignableFrom(parameterType) &&
            parameter.isAnnotated(SchemaParameter::class.java)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): FileTree {
        val project = ProjectBuilder.builder().build()
        return parameterContext.findAnnotation(SchemaParameter::class.java)
            .map(SchemaParameter::location)
            .map { location ->
                project.objects.fileTree().from(location).asFileTree
            }
            .orElse(null)
    }
}
