package io.github.leofuso.argo.plugin

import java.io.File

infix fun File.shiftLeft(content: String) = this.writeText(content).let { this }

fun applyPlugin(name: String, build: File) = build shiftLeft  "plugins { id $name } \n"

fun append(content: String, build: File) = build shiftLeft content
