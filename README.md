# Of Argo

[![Validate Gradle Wrapper](https://github.com/LeoFuso/argo/actions/workflows/gradle-wrapper-validation.yml/badge.svg)](https://github.com/LeoFuso/argo/actions/workflows/gradle-wrapper-validation.yml)
[![Build Check](https://github.com/LeoFuso/argo/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/LeoFuso/argo/actions/workflows/pre-merge.yaml)
[![Publish Plugin to Portal](https://github.com/LeoFuso/argo/actions/workflows/publish-plugin.yaml/badge.svg)](https://github.com/LeoFuso/argo/actions/workflows/publish-plugin.yaml)
[![License](https://img.shields.io/github/license/cortinico/kotlin-android-template.svg)](LICENSE) ![Language](https://img.shields.io/github/languages/top/cortinico/kotlin-android-template?color=blue&logo=kotlin)

A [Gradle](http://www.gradle.org/) plugin aimed to help working with [Apache Avro](http://avro.apache.org/). 
It supports Java code generation from JSON schema declaration files(.avsc), JSON protocol declaration files(.avpr), and Avro IDL files. 

In the future, it should support Schema Registry integration, as well.

## Of Quick Facts

If you're looking for examples of how to set up this plugin,
there are a few [use-cases](https://github.com/LeoFuso/argo/tree/main/use-cases) available for you to look.  

There are two use-cases covered by this plugin, **Code Generation** and **Schema Registry integration**.
Those are segregated into two separated namespaces: **Columba** and **Navis**. 

At this moment, just the **Columba** portion has some functionalities ready for use.  

### Of Columba

You can learn about Columba in more depth [here](columba.md).

Add the following to your build files. Substitute the desired version based on your needs.

`settings.gradle`
```groovy
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

`build.gradle`
```groovy
plugins {
    id 'io.github.leofuso.argo' version 'VERSION'
}
```

... and that's it!

# Of Kotlin DSL Support

Special notes relevant to using this plugin via the Gradle Kotlin DSL:

* Apply the plugin declaratively using the `plugins {}` block. Otherwise, various features mayn't work as intended. 
* See [Configuring Plugins in the Gradle Kotlin DSL](https://github.com/gradle/kotlin-dsl/blob/master/doc/getting-started/Configuring-Plugins.md) for more details.
* Configuration in the `avro {}` block must be applied differently than in the Groovy DSL.

## Of License

This plugin is licensed under the MIT License. See the [License](LICENSE) file for details.

