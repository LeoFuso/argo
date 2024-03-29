# Of Argo

[![Analysis](https://github.com/LeoFuso/argo/actions/workflows/analysis.yaml/badge.svg)](https://github.com/LeoFuso/argo/actions/workflows/analysis.yaml)
[![Compatibility](https://github.com/LeoFuso/argo/actions/workflows/compatibility.yaml/badge.svg)](https://github.com/LeoFuso/argo/actions/workflows/compatibility.yaml)
[![Publish Plugin to Portal](https://github.com/LeoFuso/argo/actions/workflows/publish-plugin.yaml/badge.svg)](https://github.com/LeoFuso/argo/actions/workflows/publish-plugin.yaml)
[![Publish CLI to Sonatype](https://github.com/LeoFuso/argo/actions/workflows/publish-cli.yaml/badge.svg)](https://github.com/LeoFuso/argo/actions/workflows/publish-cli.yaml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

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

This plugin is licensed under the Apache-2.0 License. See the [License](LICENSE) file for details.

