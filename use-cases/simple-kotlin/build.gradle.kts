
plugins {
    java
    idea
    kotlin("jvm") version "1.8.0"
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.argo.release)
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
