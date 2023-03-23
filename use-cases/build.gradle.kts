
plugins {
    base
    idea
    java
    kotlin("jvm") version "1.8.0" apply(false)
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.argo.release) apply(false)
}
