@file:Suppress("unused")

package io.github.leofuso.argo.plugin

/**
 * The negation of [CharSequence.isNullOrBlank].
 */
fun CharSequence?.isNotNullOrBlank(): Boolean {
    return !this.isNullOrBlank()
}

fun unsupported(reason: String): Nothing = throw UnsupportedOperationException("This operation is not supported: $reason")
