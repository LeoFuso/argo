plugins {
    java
    idea
    kotlin("jvm") version "1.8.0"
    id("io.github.leofuso.argo")
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_11.ordinal)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
