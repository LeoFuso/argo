package io.github.leofuso.columba.cli.fixtures

import java.io.File

fun String.toSysPath() = this.replace("\\", File.separator)
