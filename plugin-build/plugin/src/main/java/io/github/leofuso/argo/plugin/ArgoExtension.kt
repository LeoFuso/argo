package io.github.leofuso.argo.plugin

import org.apache.avro.Conversion
import org.apache.avro.LogicalTypes
import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class ArgoExtension {

    @Nested
    abstract fun getColumba(): ColumbaOptions

    @Nested
    abstract fun getNavis(): NavisOptions

    fun columba(action: Action<in ColumbaOptions>) = action.invoke(getColumba())

    fun navis(action: Action<in NavisOptions>) = action.invoke(getNavis())

}

interface NavisOptions

abstract class ColumbaOptions {

    abstract fun getExcluded(): ListProperty<String>

    abstract fun getOutputEncoding(): Property<String>

    @Nested
    abstract fun getFields(): ColumbaeFieldOptions

    fun fields(action: Action<in ColumbaeFieldOptions>) = action.invoke(getFields())

    @Nested
    abstract fun getAccessors(): ColumbaeAccessorOptions

    fun accessors(action: Action<in ColumbaeAccessorOptions>) = action.invoke(getAccessors())

    abstract fun getAdditionalVelocityTools(): ListProperty<Class<*>>

    abstract fun getVelocityTemplateDirectory(): DirectoryProperty

    abstract fun getAdditionalLogicalTypeFactories(): ListProperty<Class<out LogicalTypes.LogicalTypeFactory>>

    abstract fun getAdditionalConverters(): ListProperty<Class<out Conversion<*>>>

}


abstract class ColumbaeFieldOptions(@Inject val project: Project) {

    private val useDecimalTypeProperty = project.objects.property<Boolean>()

    val useDecimalTypeProvider: Provider<Boolean>
        get() = useDecimalTypeProperty

    @get:Internal
    var useDecimalType: Boolean
        get() = useDecimalTypeProperty.get()
        set(value) = useDecimalTypeProperty.set(value)

    abstract fun getStringType(): Property<StringType>

    abstract fun getVisibility(): Property<FieldVisibility>

}

abstract class ColumbaeAccessorOptions(@Inject val project: Project) {

    private val noSetterProperty = project.objects.property<Boolean>()
    private val addExtraOptionalGettersProperty = project.objects.property<Boolean>()
    private val useOptionalGettersProperty = project.objects.property<Boolean>()

    val noSetterProvider: Provider<Boolean>
        get() = noSetterProperty

    val addExtraOptionalGettersProvider: Provider<Boolean>
        get() = addExtraOptionalGettersProperty

    val useOptionalGettersProvider: Provider<Boolean>
        get() = useOptionalGettersProperty

    @get:Internal
    var noSetters: Boolean
        get() = noSetterProperty.get()
        set(value) = noSetterProperty.set(value)

    @get:Internal
    var addExtraOptionalGetters: Boolean
        get() = addExtraOptionalGettersProperty.get()
        set(value) = addExtraOptionalGettersProperty.set(value)

    @get:Internal
    var useOptionalGetters: Boolean
        get() = useOptionalGettersProperty.get()
        set(value) = useOptionalGettersProperty.set(value)

    abstract fun getOptionalGettersStrategy(): Property<OptionalGettersStrategy>

}

/**
 * Used to specify the strategy for nullable fields for generated code.
 */
enum class OptionalGettersStrategy {
    ALL_FIELDS, ONLY_NULLABLE_FIELDS
}
