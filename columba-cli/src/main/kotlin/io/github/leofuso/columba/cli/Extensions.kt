package io.github.leofuso.columba.cli

import org.apache.avro.Protocol
import java.io.File

fun Protocol.path(): String =
    namespace.replace(NAMESPACE_SEPARATOR, File.separator) + File.separator + name + EXTENSION_SEPARATOR + PROTOCOL_EXTENSION
