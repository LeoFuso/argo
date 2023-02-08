package io.github.leofuso.argo.plugin.compiler.exception

class NonDeterministicSchemaResolutionException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, exception: Exception) : super(message, exception)
}
