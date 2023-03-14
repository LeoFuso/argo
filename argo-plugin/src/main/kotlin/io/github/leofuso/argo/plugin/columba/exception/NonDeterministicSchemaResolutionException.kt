package io.github.leofuso.argo.plugin.columba.exception

class NonDeterministicSchemaResolutionException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, exception: Exception) : super(message, exception)
}
