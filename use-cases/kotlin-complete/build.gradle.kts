plugins {
    java
    idea
    kotlin("jvm") version "1.8.0"
    id("io.github.leofuso.argo")
}

dependencies {

    implementation("io.github.leofuso.argo:custom-tools:0.0.1-SNAPSHOT")
    implementation("io.github.leofuso.argo:custom-tools:0.0.1-SNAPSHOT")
    implementation("org.apache.avro:avro:1.11.1")

    compileApacheAvroJava("io.github.leofuso.argo:custom-tools:0.0.1-SNAPSHOT")
    compileApacheAvroJavaSources("io.github.leofuso.argo:custom-tools:0.0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(17)
}

argo {
    columba {

        getCompiler().set("org.apache.avro:avro-compiler:1.11.1")
        getOutputEncoding().set("UTF-8")
        getExcluded().add("**/*Json.avsc")

        getVelocityTemplateDirectory().set(File("templates/custom/"))
        velocityTool("io.github.leofuso.argo.custom.TimestampGenerator")
        converter("io.github.leofuso.argo.custom.TimeZoneConversion")
        logicalTypeFactory("timezone", "io.github.leofuso.argo.custom.TimeZoneLogicalTypeFactory")

        fields {
            getVisibility().set("PRIVATE")
            useDecimalType = true
            getStringType().set("String")
        }
        accessors {
            noSetters = true
            addExtraOptionalGetters = false
            useOptionalGetters = true
            optionalGettersForNullableFieldsOnly = true
        }
    }
}
