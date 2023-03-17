package io.github.leofuso.columba.cli.fixtures.annotation

import org.junit.jupiter.api.extension.ParameterResolver

/**
 * An annotation aimed to pass metadata needed by [ParameterResolver].
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Directory(
    /**
     * JSON-style template *location* to be loaded.
     *
     * @see ParameterResolver
     */
    val location: String = ""
)
