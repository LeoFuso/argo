package io.github.leofuso.columba.cli.command

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.path
import io.github.leofuso.columba.cli.CLASS_EXTENSION
import io.github.leofuso.columba.cli.VELOCITY_TEMPLATE_EXTENSION

class VelocityOptions : OptionGroup(
    name = "Velocity Options",
    help = """
        
           Velocity is a Java-based templating engine. It is used by the compiler to generate Java source code.
           
           You can costumize its behavior by providing additional velocity-tools(.$CLASS_EXTENSION)
           alongside a corresponding template(.$VELOCITY_TEMPLATE_EXTENSION).
           
        """
) {
    val velocityTemplate by option(
        "--velocity-template",
        "-t",
        help = """
           
           Use this option to locate the custom VelocityTemplates(.$VELOCITY_TEMPLATE_EXTENSION) 
           by providing its path.
           
        """
    )
        .path(
            mustBeReadable = true,
            mustExist = true,
            canBeFile = false
        )

    val additionalVelocityTools by option(
        "--velocity-tools",
        "-v",
        help = """
          
           Use this option to add additional Velocity tool-classes by 
           passing the fully qualified class names, separated by ';'.
           
        """
    )
        .split(";")
        .default(listOf())
        .unique()
}
