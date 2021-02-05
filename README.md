[![License](http://img.shields.io/badge/license-Apache%202.0-brightgreen.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0) [![Build Status](http://img.shields.io/travis/ewerk/gradle-plugins.svg?style=flat)](https://travis-ci.org/ewerk/gradle-plugins)
### JAXB2 plugin
This is a fork from [ewerk/gradle-plugins/jaxb2](https://github.com/ewerk/gradle-plugins/).

~~The thing we needed to be fixed has been fixed upstreams, so please migrate back to [ewerk/gradle-plugins/jaxb2](https://github.com/ewerk/gradle-plugins/).~~
There are additional things to work on.


#### Description

This plugin makes it easy to generate Java source code from XML schema files (.xsd). Internally
the plugin relies on the JAXB2 ant task for generating the code. The examples below show how 
the plugin can be used.

At the moment the plugin is simplistic and just supports creating Java code from XSD. JAXB2
specific special stuff like binding files or additional task parameters are currently not 
supported but can surely make it into the plugin in future. 

Please have a look at the plugins [change log](change_log.md).

#### Dependency management
The plugin creates a own 'jaxb2' configuration and adds it to the project. Then it adds all needed
dependencies to execute the JAXB ant task. You may need to extend are change these dependencies.
As we have the `jaxb2` configuration, you can do this by using the default `dependencies {}` 
closure of your project. This is shown in the examples below.

#### Configuration

##### taskName
The full qualified name of the JAXB2 ant task the does the real work. 
Defaults to `com.sun.tools.xjc.XJCTask`. 
Normally there will be need to change this.

##### xjc
This is the container for configuring the distinct generation steps. It can be repeated as needed
within the `jaxb2` extension. The container structure is as follows:

```groovy
xjc {
  'generation-step-name' {
    // optional, defaults to src/generated/java
    generatedSourcesDir = 'any/relative/path'
    
    // full qualified base package for the classes to be generated
    basePackage = 'com.any.app'
    
    // relative path the XSD file to generate the code from
    schema = 'src/main/xsd/any-file.xsd'
    
    // relative path to directory including binding files
    // all files of pattern **/*.xjb will be included
    // default to null (no binding files used)
    // Optional, can be left away
    bindingsDir = 'src/main/xsb'
    
    // comma separated list of binding file includes
    // falls back to '**/*.xjb' if not specified
    includedBindingFiles = 'any.xjb, subdir/test.xjb'
    
    additionalArgs = '-nv -dtd'

    // output encoding of the generated files
    encoding = 'UTF-8'
  }
}
```

#### Examples

__Use via Gradle plugin portal__

```groovy
plugins {
  id 'com.github.gradlecommunity.jaxb2' version '0.1.0'
}
```

__Full configuration example__

```groovy
plugins {
  id 'com.github.gradlecommunity.jaxb2' version '0.1.0'
}

repositories {
  mavenCentral()
}

// the whole dependency container could be left out
dependencies {
  // default JAXB libs added by the plugin
  jaxb2 'com.sun.xml.bind:jaxb-xjc:3.0.0'
  
  // any custom libraries here
  jaxb2 'com. …'
}

jaxb2 {
  xjc {
    'request-classes' {
      basePackage = 'com.any.app.model.request'
      schema = 'src/main/xsd/request.xsd'
      encoding = 'UTF-8'
      extension = true 
    }
  }

  xjc {
    'response-classes' {
      basePackage = 'com.any.app.model.response'
      schema = 'src/main/xsd/response.xsd'
      bindings = 'src/main/xsb'
    }
  }
}

```

__Minimal configuration example__
```groovy
plugins {
  id 'com.github.gradlecommunity.jaxb2' version '1.0.2'
}

repositories {
  mavenCentral()
}

jaxb2 {
  xjc {
    'request-classes' {
      basePackage = 'com.any.app.model.request'
      schema = 'src/main/xsd/request.xsd'
    }
  }
}
```
