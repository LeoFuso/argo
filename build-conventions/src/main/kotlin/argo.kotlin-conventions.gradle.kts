@file:Suppress("UnstableApiUsage", "SpellCheckingInspection")

import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    base
    jacoco
    id("org.jetbrains.kotlin.jvm")
    id("org.jmailen.kotlinter")
    id("io.gitlab.arturbosch.detekt")
    id("org.sonarqube")
}

val versionCatalog: VersionCatalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
val detektVersion: String = versionCatalog.findVersion("detekt").get().requiredVersion

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
        finalizedBy(tasks.sonar)
    }

    test {

        jvmArgs(
            "-Xss320k",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED"
        )

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

sonar {
    properties {
        property("sonar.projectName", project.name)
        property("sonar.projectKey", project.group)
        property("sonar.projectDescription", extra["local.description"] as String)
        property("sonar.organization", "leofuso")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.links.scm", "https://github.com/LeoFuso/argo")
        property("sonar.sourceEncoding", "UTF-8")
    }
    if (System.getenv("SONAR_TOKEN") == null) {
        isSkipProject = true
    }
}
