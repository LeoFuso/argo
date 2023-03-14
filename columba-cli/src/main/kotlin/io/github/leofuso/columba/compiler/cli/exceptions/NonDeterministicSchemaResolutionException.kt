package io.github.leofuso.columba.compiler.cli.exceptions

class NonDeterministicSchemaResolutionException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, exception: Exception) : super(message, exception)
}
