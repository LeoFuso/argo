plugins {
    java
    idea
    kotlin("jvm") version "1.8.0"
    id("io.github.leofuso.argo")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
