import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    jacoco
    id("org.jetbrains.kotlin.jvm")
    id("org.jmailen.kotlinter")
    id("io.gitlab.arturbosch.detekt")
    id("org.sonarqube")
}

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.22.0")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.22.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_11.ordinal)
}

tasks {

    withType<Detekt>().configureEach {
        jvmTarget = "11"
        reports {
            html.required.set(true)
            txt.required.set(true)
            md.required.set(true)
        }
    }

    withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            freeCompilerArgs.set(
                listOf(
                    "-Xjsr305=strict",
                    "-opt-in=kotlin.RequiresOptIn",
                    "-Werror",
                    "-verbose"
                )
            )
        }
    }

    detekt.configure {
        reports {
            xml.required.set(true)
            html.required.set(true)
            txt.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
        }
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        dependsOn(tasks.test)
        finalizedBy(tasks.sonar)
    }

    test {

        jvmArgs("-Xss320k")
        minHeapSize = "120m"
        maxHeapSize = "280m"
        maxParallelForks = Runtime.getRuntime().availableProcessors().div(2)

        useJUnitPlatform()
        outputs.upToDateWhen { false }
        finalizedBy(tasks.jacocoTestReport)
    }

    check {
        dependsOn(tasks.lintKotlin)
    }
}

kotlinter {
    ignoreFailures = false
    reporters = arrayOf("plain")
    experimentalRules = true
    disabledRules = arrayOf(
        "no-empty-first-line-in-method-block",
        "no-blank-line-before-rbrace"
    )
}

detekt {
    config = files("${rootDir.parentFile.absolutePath}/build-conventions/detekt.yml")
}

sonar {
    properties {
        property("sonar.projectKey", "io.github.leofuso.argo")
        property("sonar.organization", "leofuso")
        property("sonar.host.url", "https://sonarcloud.io")
    }
    if (System.getenv("SONAR_TOKEN") == null) {
        isSkipProject = true
    }
}
