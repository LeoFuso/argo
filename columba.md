# Of Columba

...and its inner workings.

**Disclaimer**: Functionality on **Windows OS** has not yet been verified, but should work as intended.

### Of its usage

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

See all [available versions](CHANGELOG.md).

The plugin performs its tasks with **process isolation**, meaning you can run **Columba** with a different
[Toolchain](https://docs.gradle.org/current/userguide/toolchains.html#specify_custom_toolchains_for_individual_tasks).
Another advantage is that all the dependencies related to code generation are consumed in isolation, as well. 

The plugin automatically applies some dependencies needed in its runtime, but it does so with classpath isolation. 
In other words, it shouldn't get in the way of conflicting dependencies.

You can, also, customize the Avro compiler dependency, as well as the Columba version, as you may need different versions in runtime and
during the source code generation:

`build.gradle`
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.apache.avro:avro:1.11.0'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(19)
    }
}

argo {
    columba {
        // custom toolchain
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
        version = '0.1.2-SNAPSHOT' // Columba CLI version
        compilerVersion = '1.11.1' // avro-compiler version
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

By default, the tasks infer the location of the source files to be `src/avro`, even tho this behavior can be customized, 
see [External Sources](#sources).

All _Source Generation_ tasks should be ordered in a way that the plugin tasks run before anything else, out of the box.
Running `gradle build` should be enough to your project setup.

## Of Columba parser

There is no required pre-defined order of appearence for the source files, nor should you worry about inline definitions vs separeted
file definitions, and duplicated resolutions are ignored by default.
The parser component of the plugin tries its best to ensure a smooth experience regarding Schema dependencies, but the parser can fail.
If that's the case, please open an issue about it!

## Of external dependencies

### Sources

Should you depend on external JSON schema declaration files(.avsc), JSON protocol declaration files(.avpr) or Avro IDL files(.avdl),
you can extend the classpath scan by providing the right configurations, see below:

`build.gradle`
```groovy
dependencies {
    
    /* Protocol dependencies */
    generateApacheAvroProtocol someJar.outputs.files // main SourceSet
    generateTestApacheAvroProtocol 'io.github.leofuso.events:avro-events:1.0.0' // test SourceSet

    /* Schema sources (.avpr, .avsc) */
    compileApacheAvroJavaSources 'io.github.leofuso.events:avro-events:1.0.0' // main SourceSet
    compileTestApacheAvroJavaSources someJar.outputs.files // test SourceSet
}
```

There are as many configurations as there are SourceSets.

### Tools

Should you want to import [custom conversions, LogicalTypeFactories](https://avro.apache.org/docs/1.11.1/specification/#logical-types)
and [VelocityTools](https://velocity.apache.org/tools/3.1/) during the Code Generation phase, there are specific configuraions to do so, see below:

`build.gradle`
```groovy
 
plugins {
    id 'java'
    id 'io.github.leofuso.argo'
}

repositories {
    mavenCentral()
}

dependencies {
    compileApacheAvroJava files('/usr/your-user/downloads/some-jar.jar')
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

argo {
    columba {
        logicalTypeFactory('timezone', 'io.github.leofuso.argo.custom.TimeZoneLogicalTypeFactory')
        converter('io.github.leofuso.argo.custom.TimeZoneConversion')
        velocityTemplateDirectory = file('templates/custom/')
        velocityTool('io.github.leofuso.argo.custom.TimestampGenerator')
        velocityTool('io.github.leofuso.argo.custom.CommentGenerator')
        fields {
            stringType = 'Utf8'
        }
    }
}
```
Keep in mind that all dependencies added to `compileApacheAvroJava` are also added to `implementation`.
Since additional **LogicalTypeFactories** or **CustomConversions** should be present in the `classpath` during
the task run, you can't use a custom conversion first declared on the source code that the build targets.

You can use a multiple module configuration to achieve this, or have it in a separate `jar` file.

Obs. the `implementation` is needed so that the code that references the custom conversion can compile.

## Of Options

The following is all available customizations, alongside its default values.
Since the plugin relly on [SpecificCompiler](https://github.com/justinsb/avro/blob/master/src/java/org/apache/avro/specific/SpecificCompiler.java)
implementation, all configurations are passed _as is_ to the compiler.

`build.gradle`
```groovy

argo {
    columba {
        
        toolchain {
            languageVersion = JavaLanguageVersion.of(19)
        }
        
        version = '1.11.0' // necessary due to the Columba Cli usage.
        compilerVersion = '1.11.1' // necessary due to the SpecificCompiler usage.
        outputEncoding = 'UTF-8' // Encoding for the generated classes.
        
        logicalTypeFactory('factory', 'SomeFactory')
        converter('SomeConverter')
        velocityTemplateDirectory = file('templates/custom/')
        velocityTool('AVelocityTool')
        
        fields {
            visibility = 'PRIVATE' // Java class property field visibility, either PRIVATE or PUBLIC. 
            useDecimalType = true // either or not to use BigDecimal as decimal type, instead of ByteArray.
            stringType = 'CharSequence' // String property implementing class, either CharSequence, String, Utf8.
        }
        accessors {
            noSetters = false // all properties final
            addExtraOptionalGetters = false // extra getters on top of default ones
            useOptionalGetters = true // use optional instead of default getters
            optionalGettersForNullableFieldsOnly = true // optional getters only for nullable fields
        }
    }
}
```

[Decimal type ref](https://avro.apache.org/docs/1.11.1/specification/#decimal).

As you can see it, it was a deliberate choice to reference the actual classes that the SpecificCompiler uses, to prevent miss config
associated with typos.

### Option details

To be detailed.

## Of Compatibility

All tests were performed using the following specs:

| Gradle | Java | Compiler                               |
|--------|------|----------------------------------------|
| 7.6    | 17   | `org.apache.avro:avro-compiler:1.11.1` |


## Of IntelliJ integration

The plugin attempts to make IntelliJ play more smoothly with generated sources when using Gradle-generated project files.
However, there are still some rough edges.
It should work better if you first run `gradle build`, and _after_ that run `gradle idea`.

# Of Kotlin Support

The Java classes generated from your Avro files should be automatically accessible in the classpath to Kotlin classes in the same SourceSet,
and transitively to any SourceSets that depend on that SourceSet.
This is accomplished by this plugin detecting that the Kotlin plugin has been applied and informing the Kotlin compilation tasks
of the generated sources presence for cross-compilation.

This plugin doesn't support producing the Avro generated classes as Kotlin classes, as that functionality is not
provided by the upstream Avro library (the compiler).

## DSL

One can find examples of how to apply the plugin using _Kotlin DSL_ [here](use-cases/kotlin-complete/build.gradle.kts).

## Of Open Source and the Community

The development of this plugin in a direct response to [David's plugin](https://github.com/davidmc24/gradle-avro-plugin)
of the same functionality.
Any resemblence is not a coincidence. I've used David's plugin as a starting point, and copy some
of its functionality **as is**. David, and the community surrouding their plugin, did a wonderful job,
and I'm most definitely a user of David's plugin. If you're in doubt about which plugin to use, use
David's, since it is the most battle tested, at least for now!
