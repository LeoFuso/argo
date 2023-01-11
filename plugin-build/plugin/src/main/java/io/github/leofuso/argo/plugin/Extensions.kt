package io.github.leofuso.argo.plugin

/**
 * Throws an [AssertionError] calculated by [lazyMessage] if the [value] is false.
 */
inline fun assertTrue(value: Boolean, lazyMessage: () -> Any = { "Failed Assertion." }) {
    if (!value) {
        val message = lazyMessage()
        throw AssertionError(message)
    }
}
