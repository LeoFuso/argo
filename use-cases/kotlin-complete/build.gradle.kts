plugins {
    java
    idea
    kotlin("jvm") version "1.8.0"
    id("io.github.leofuso.argo")
}

dependencies {
    compileApacheAvroJava("io.github.leofuso.argo:custom-tools:0.0.1-SNAPSHOT")
    compileApacheAvroJavaSources("io.github.leofuso.argo:custom-tools:0.0.1-SNAPSHOT")
    implementation("io.github.leofuso.argo:custom-tools:0.0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_11.ordinal)
}

argo {
    columba {

        getCompiler().set("org.apache.avro:avro-compiler:1.11.1")
        getOutputEncoding().set("UTF-8")

        getAdditionalVelocityTools().add("io.github.leofuso.argo.custom.TimestampGenerator")
        getVelocityTemplateDirectory().set(File("templates/custom/"))
        getAdditionalConverters().add("io.github.leofuso.argo.custom.TimeZoneConversion")
        getAdditionalLogicalTypeFactories().put("timezone", "io.github.leofuso.argo.custom.TimeZoneLogicalTypeFactory")

        fields {
            getVisibility().set(org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility.PRIVATE)
            useDecimalType = true
            getStringType().set(org.apache.avro.generic.GenericData.StringType.String)
        }
        accessors {
            noSetters = true
            addExtraOptionalGetters = false
            useOptionalGetters = true
            optionalGettersForNullableFieldsOnly = true
        }
    }
}
