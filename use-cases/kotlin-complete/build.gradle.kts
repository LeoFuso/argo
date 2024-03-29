
plugins {
    idea
    java
    kotlin("jvm") version "1.8.0"
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.argo.release)
}

dependencies {

    implementation("org.apache.avro:avro:1.11.1")
    compileApacheAvroJava(project(":external-tools")) {
        isTransitive = false
    }
    compileApacheAvroJavaSources(project(":external-tools"))
    generateApacheAvroProtocol(project(":external-tools"))
}

kotlin {
    jvmToolchain(17)
}

argo {
    columba {

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
