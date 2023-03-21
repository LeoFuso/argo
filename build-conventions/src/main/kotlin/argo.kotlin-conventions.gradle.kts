@file:Suppress("UnstableApiUsage")

import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    jacoco
    id("org.jetbrains.kotlin.jvm")
    id("org.jmailen.kotlinter")
    id("io.gitlab.arturbosch.detekt")
}

val versionCatalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
val detektVersion = versionCatalog.findVersion("detekt").get().requiredVersion

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:$detektVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:$detektVersion")
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
}

tasks {

    withType<Detekt>().configureEach {
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
        "no-blank-line-before-rbrace",
        "no-wildcard-imports"
    )
}

detekt {
    config = files("..${File.separator}gradle${File.separator}detekt.yml")
}

