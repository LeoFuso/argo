package io.github.leofuso.argo.plugin.compiler.arguments

internal sealed interface Argument {

    fun args(): List<String>

}
