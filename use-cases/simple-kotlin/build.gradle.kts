
plugins {
    java
    idea
    kotlin("jvm") version "1.8.0"
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.argo.release)
    id("com.github.imflog.kafka-schema-registry-gradle-plugin") version "1.9.1"

}

repositories {}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

schemaRegistry {
    url.set("http://localhost:9081/")

    register {
        subject("obs.receipt.avsc", "simple-kotlin/src/main/avro/obs.receipt.avsc", "AVRO")
            .addLocalReference("io.github.leofuso.obs.demo.events.ReceiptLine", "simple-kotlin/src/main/avro/obs.receipt-line.avsc")
            .addReference("io.github.leofuso.obs.demo.events.StatementLine", "obs.statement-line", 1)
            //.addLocalReference("io.github.leofuso.obs.demo.events.StatementLine", "simple-kotlin/src/main/avro/obs.statement-line.avsc")
    }
}
