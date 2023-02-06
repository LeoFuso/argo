import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jmailen.kotlinter")
    id("io.gitlab.arturbosch.detekt")
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
                    //"-Werror",
                    "-verbose"
                )
            )
        }
    }

    test {
        useJUnitPlatform()
    }
}


kotlinter {
    ignoreFailures = false
    reporters = arrayOf("plain")
    experimentalRules = true
    disabledRules = emptyArray()
}

detekt {
    config = files("${rootDir.parentFile.absolutePath}/build-conventions/detekt.yml")
}