plugins {
    java
    idea
    kotlin("jvm") version "1.8.0"
    id("io.github.leofuso.argo")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
