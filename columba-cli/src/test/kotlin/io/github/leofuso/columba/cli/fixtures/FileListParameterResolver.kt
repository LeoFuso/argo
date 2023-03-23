package io.github.leofuso.columba.cli.fixtures

import io.github.leofuso.columba.cli.fixtures.annotation.Directory
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File

class FileListParameterResolver : ParameterResolver {

    override fun supportsParameter(parameter: ParameterContext, extension: ExtensionContext): Boolean {
        val subject = parameter.parameter
        val parameterType = subject.type
        return File::class.java.isAssignableFrom(parameterType) &&
            parameter.isAnnotated(Directory::class.java)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): File {
        return parameterContext.findAnnotation(Directory::class.java)
            .map(Directory::location)
            .map { location ->
                loadResource(location)
            }
            .orElse(null)
    }
}
