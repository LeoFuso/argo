package io.github.leofuso.columba.cli.command

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class AccessorOptions : OptionGroup(
    name = "Accessor Options",
    help = "Options related to Java field-accessor methods."
) {
    val allowSetters by option(
        "--allow-setters",
        help = "Whether the properties are final."
    ).flag(default = false)

    val addExtraOptionalGetters by option(
        "--add-extra-optional-getters",
        help = "Add extra optional-wrapped getters on top of default ones."
    ).flag(default = false)

    val useOptionalGetters by option(
        "--use-optional-getters-only",
        help = "Use optional-wrapped getters instead of default ones."
    ).flag(default = false)

    val useOptionalGettersForNullableFieldsOnly by option(
        "--use-optional-getters-for-nullable-fields-only",
        help = "Use optional-wrapped getters instead of default ones, but only for fields that are nullable."
    ).flag(default = false)
}
