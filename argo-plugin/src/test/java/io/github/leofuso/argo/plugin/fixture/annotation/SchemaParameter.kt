package io.github.leofuso.argo.plugin.fixture.annotation

import org.junit.jupiter.api.extension.ParameterResolver

/**
 * An annotation aimed to pass metadata needed by [ParameterResolver].
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SchemaParameter(
    /**
     * JSON-style template *location* to be loaded.
     *
     * @see ParameterResolver
     */
    val location: String = ""
)
