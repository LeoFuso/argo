package io.github.leofuso.navis.cli.command

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.option
import java.util.regex.Pattern

class ClientOptions : OptionGroup(
    name = "SchemaRegistry Client standard options",
    help = "Options related to the SchemaRegistry Client itself."
) {

    private val pattern: Pattern = Pattern.compile("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
    val url by option(
        "--url",
        "-i",
        help = """
            
            SchemaRegistry server unique HTTP URL.
            The protocol part is obligatory.
            
        """
    ).check("Malformed URL.") { input -> pattern.matcher(input).matches() }

    val properties by option(
        "--property",
        "-p",
        help = """
            
            Use this option to include all needed `properties` to configure the SchemaRegistry's Client.
             
            Example:
           
            ```
            --property basic.auth.credentials.source=USER_INFO
            -p         basic.auth.user.info=username:password
            
            ```
        """
    )
        .associate()
        .check("Neither the property's name nor value can be blank.") { input ->
            input.all { (key, value) -> key.isNotBlank() && value.isNotBlank() }
        }

    val headers by option(
        "--header",
        "-h",
        help = """
            
            Use this option to include extra HTTP Headers needed to communicate with the SchemaRegistry server.
             
            Example:
           
            ```
            --header User-Agent: Chrome
            -h       Accepted-Language: pt-BR, es-MX
            
            ```
        """
    )
        .associate(":")
        .check("Neither the header's name nor value can be blank.") { input ->
            input.all { (key, value) -> key.isNotBlank() && value.isNotBlank() }
        }

}
