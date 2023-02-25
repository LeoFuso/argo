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

### Of Columba and its customizations

To customize the **Columba** build, one must import the necessary _avro_ dependencies as part of the _buildscript_.
```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath group: 'org.apache.avro', name: 'avro', version: '1.11.0'
    }
}
```

The need of this dependency is to be able to reference specific classes available to the compiler; 
those classes offer customized behavior during the compilation, e.g., the different _String_ types supported by the _Avro Protocol_.

_if this is a deal break, please create an issue reporting so, and I'll provide the necessary workaround._

### Of Columba Usage

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

Additionally, ensure that you have an implementation dependency on Avro, such as:

`build.gradle`
```groovy
repositories {
    mavenCentral()
}
dependencies {
    implementation 'org.apache.avro:avro:1.11.0'
}
```
You can, also, customize the Avro compiler dependency, as you may need different versions in runtime and
during the source code generation:

`build.gradle`
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.apache.avro:avro:1.11.0'
}

argo {
    columba {
        compiler = 'org.apache.avro:avro-compiler:1.11.1'
    }
}
```

## Of Columba tasks

After applied, the plugin should generate as many tasks as there's SourceSets in your project. If you don't know what these are, 
don't worry about it. In most cases, for Java and Kotlin, there are two SourceSets: **main** and **test**.

This basic setup generates 4 tasks: 
- **compileApacheAvroJava**
- **compileTestApacheAvroJava**
  - Both generate Java class files from JSON schema declaration files(.avsc)
    and JSON protocol declaration files(.avpr).
- **generateApacheAvroProtocol**
- **generateTestApacheAvroProtocol**
  - Both generate JSON Protocol declaration files from Avro IDL files(.avdl).

By default, the tasks infer the location of the source files to be `src/avro`, even tho this behavior can be customized.

All _Source Generation_ tasks should be ordered in a way that the plugin tasks run before anything else, out of the box.
Running `gradle build` should be enough to your project setup.

## Of Columba parser

There is no required pre-defined order of appearence for the source files, nor should you worry about inline definitions vs separeted
file definitions, and duplicated resolutions are ignored by default.
The parser component of the plugin tries its best to ensure a smooth experience regarding Schema dependencies, but the parser can fail.
If that's the case, please open an issue about it!

## Of Columba working with external dependencies

### External Sources
Should you depend on external JSON schema declaration files(.avsc), JSON protocol declaration files(.avpr) or Avro IDL files(.avdl),
you can extend the classpath scan by providing the right configurations, see below:

`build.gradle`
```groovy
dependencies {
    generateApacheAvroProtocol someJar.outputs.files
    generateTestApacheAvroProtocol 'io.github.leofuso.events:avro-events:1.0.0'
    compileApacheAvroJavaSources 'io.github.leofuso.events:avro-events:1.0.0'
    compileTestApacheAvroJavaSources someJar.outputs.files
}
```

There are as many configurations as there are SourceSets.

### External Tools

There are also configurations to import custom conversions, LogicalTypeFactories and VelocityTools 
that you may want to use during the Code Generation phase.

Here is an example of how this would look:

`build.gradle`
```groovy
import org.apache.avro.generic.GenericData
                                  
plugins {
    id 'java'
    id 'io.github.leofuso.argo'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation files('/usr/your-user/downloads/some-jar.jar')
    compileApacheAvroJava files('/usr/your-user/downloads/some-jar.jar')
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

argo {
    columba {
        additionalLogicalTypeFactories.put('timezone', 'io.github.leofuso.argo.custom.TimeZoneLogicalTypeFactory')
        additionalConverters.add('io.github.leofuso.argo.custom.TimeZoneConversion')
        velocityTemplateDirectory = file('templates/custom/')
        additionalVelocityTools = [
            'io.github.leofuso.argo.custom.TimestampGenerator',
            'io.github.leofuso.argo.custom.CommentGenerator'
        ]
        fields {
            stringType = GenericData.StringType.Utf8
        }
    }
}
```
Keep in mind that **both** `implementation` and `compileApacheAvroJava` are necessary. 
Since additional **LogicalTypeFactories** or **CustomConversions** should be present in the `classpath` during
the task run, you can't use a custom conversion first declared on the source code that the build targets.

You can use a multiple module configuration to achieve this, or have it in a separate `jar` file.

Obs. the `implementation` is needed so that the code that references the custom conversion can compile. 

## Of Columba Options

The following is all available customizations, alongside its default values.
Since the plugin relly on [SpecificCompiler](https://github.com/justinsb/avro/blob/master/src/java/org/apache/avro/specific/SpecificCompiler.java) 
implementation, all configurations are passed _as is_ to the compiler.

`build.gradle`
```groovy

import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.generic.GenericData

argo {
    columba {
        compiler = 'org.apache.avro:avro-compiler:1.11.0' // necessary due to the SpecificCompiler usage.
        outputEncoding = 'UTF-8' // Encoding for the generated classes.
        fields {
            visibility = SpecificCompiler.FieldVisibility.PRIVATE // Java class property field visibility, either PRIVATE or PUBLIC. 
            useDecimalType = true // either or not to use BigDecimal as decimal type, instead of ByteArray.
            stringType = GenericData.StringType.CharSequence // String property implementing class, either CharSequence, String, Utf8.
        }
        accessors {
            noSetters = false // all properties final
            addExtraOptionalGetters = false // extra getters on top of default ones
            useOptionalGetters = true // use optional instead of default getters
            optionalGettersForNullableFieldsOnly = true // optional getters only for nullable fields
        }
    }
    navis { /* empty*/ }
}
```

[Decimal type ref](https://avro.apache.org/docs/1.11.1/specification/#decimal).

As you can see it, it was a deliberate choice to reference the actual classes that the SpecificCompiler uses, to prevent miss config
associated with typos.

### Of External Tools

You can customize the compiler by providing additional Velocity Tools, custom LogicalTypeFactories
and custom type conversions.
By default, those configurations are all empty.  

`build.gradle`
```groovy

dependencies {
    implementation files('/usr/your-user/downloads/some-jar.jar')
    compileApacheAvroJava files('/usr/your-user/downloads/some-jar.jar')
}

argo {
    columba {
        additionalLogicalTypeFactories.put('timezone', 'io.github.leofuso.argo.custom.TimeZoneLogicalTypeFactory')
        additionalConverters.add('io.github.leofuso.argo.custom.TimeZoneConversion')
        velocityTemplateDirectory = file('templates/custom/')
        additionalVelocityTools = [
            'io.github.leofuso.argo.custom.TimestampGenerator',
        ]
    }
    navis { /* empty*/ }
}
```

### Of Columba option details

To be defined.

## Compatibility

To be defined.


## Of IntelliJ integration

The plugin attempts to make IntelliJ play more smoothly with generated sources when using Gradle-generated project files.
However, there are still some rough edges.
It should work better if you first run `gradle build`, and _after_ that run `gradle idea`.

# Kotlin Support

The Java classes generated from your Avro files should be automatically accessible in the classpath to Kotlin classes in the same SourceSet,
and transitively to any SourceSets that depend on that SourceSet.
This is accomplished by this plugin detecting that the Kotlin plugin has been applied and informing the Kotlin compilation tasks 
of the generated sources presence for cross-compilation.

This plugin doesn't support producing the Avro generated classes as Kotlin classes, as that functionality is not 
currently provided by the upstream Avro library (the compiler).

# Kotlin DSL Support

Special notes relevant to using this plugin via the Gradle Kotlin DSL:

* Apply the plugin declaratively using the `plugins {}` block. Otherwise, various features mayn't work as intended. 
* See [Configuring Plugins in the Gradle Kotlin DSL](https://github.com/LeoFuso/argo) for more details.
* Configuration in the `avro {}` block must be applied differently than in the Groovy DSL. See the example below for details.

## Of License

This plugin is licensed under the MIT License. See the [License](LICENSE) file for details.

## Of Open Source and the Community

The development of this plugin in a direct response to [David's plugin](https://github.com/davidmc24/gradle-avro-plugin)
of the same functionality.
Any resemblence is not a coincidence. I've used David's plugin as a starting point, and copy some 
of its functionality **as is**. David, and the community surrouding their plugin, did a wonderful job,
and I'm most definitely a user of David's plugin. If you're in doubt about which plugin to use, use
David's, since it is the most battle tested, at least for now!

