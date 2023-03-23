package io.github.leofuso.columba.cli.command

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.unique
import javax.lang.model.SourceVersion

class ConverterOptions : OptionGroup(
    name = "Converter Options",
    help = """
        
           Conversion between generic and logical type values.
           
        """
) {
    val additionalLogicalTypeFactories by option(
        "--logical-type-factories",
        "-f",
        help = """
            
            Instances of these classes are added to a LogicalTypeFactory pool to convert logical types to
            a particular representation.
          
            Use this option to add LogicalTypeFactories by 
            passing its name, and the fully qualified class name. 
           
           Example:
           
            ```
            --logical-type-factories timezone=io.github.leofuso.tools.TimeZoneLogicalTypeFactory
            --logical-type-factories other=io.github.leofuso.tools.OtherLogicalTypeFactory
            -f other-one=io.github.leofuso.tools.OtherOneLogicalTypeFactory
            ```
            
        """
    )
        .associate()
        .check("For each item, the factory name must not be blank, and the class name must be in the fully qualified form.") { input ->
            input.all { (key, value) -> key.isNotBlank() && SourceVersion.isName(value) }
        }

    val additionalConverters by option(
        "--converters",
        "-c",
        help = """
            
            Instances of these classes are added to GenericData to convert logical types to
            a particular representation.
          
            Use this option to add additional converter classes by 
            passing the fully qualified class names, separated by ';'.
            
        """
    )
        .split(";")
        .default(listOf())
        .unique()
        .check("For each item, the class name must be in the fully qualified form.") { input -> input.all(SourceVersion::isName) }
}
