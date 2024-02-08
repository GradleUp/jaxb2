package com.gradleup.jaxb.tasks

import com.gradleup.jaxb.Jaxb2Plugin
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import static org.gradle.api.logging.Logging.getLogger

/**
 * Task that does the actual generation stuff. Declares the Ant task and then runs it for all
 * configured {@link XjcTaskConfig} objects.
 *
 * xjc https://jaxb.java.net/2.2.4/docs/xjcTask.html
 * depends/produces is used for incremental compilation
 *
 * @author holgerstolzenberg
 * @since 1.0.0
 */
@CacheableTask
abstract class GenerateJaxb2Classes extends DefaultTask {
  private static final Logger LOG = getLogger(GenerateJaxb2Classes.class)

  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  abstract RegularFileProperty getSchemaFile()

  @Input
  abstract Property<String> getBasePackage()

  @Input
  abstract Property<String> getEncoding()

  @Input
  abstract Property<Boolean> getExtension()

  @Input
  abstract Property<String> getAdditionalArgs()

  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  abstract RegularFileProperty getCatalogFile()

  @InputFiles
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  abstract ConfigurableFileCollection getBindingsFiles()

  @Input
  abstract Property<Boolean> getHeader()

  @OutputDirectory
  abstract DirectoryProperty getGeneratedSourcesDirectory()

  GenerateJaxb2Classes() {
    this.group = Jaxb2Plugin.TASK_GROUP
  }

  @SuppressWarnings("GroovyUnusedDeclaration")
  @TaskAction
  antXjc() {
    ant.taskdef(
        name: 'xjc',
        classname: project.extensions.jaxb2.taskName,
        classpath: project.configurations.jaxb2.asPath)

    // Transform package to directory location to specify depends/produces when multiple schema output to same generatedSourcesDir
    // Changing one schema will only cause recompilation/generation of that schema
    def generatedSourcesDirPackage = new File(generatedSourcesDirectory.get().asFile,
            basePackage.get().replace(".", "/"))

    def arguments = [
            destdir  : generatedSourcesDirectory.get().asFile,
            package  : basePackage.get(),
            schema   : schemaFile.get().asFile,
            encoding : encoding.get(),
            extension: extension.get(),
            header   : header.get(),
    ]

    if (catalogFile.isPresent()) {
      arguments.catalog = catalogFile.get().asFile
    }

    // the depends and produces is compared using the time-stamp of the schema file and the destination package folder
    ant.xjc(arguments) {
      depends(file: schemaFile.get().asFile)
      produces(dir: generatedSourcesDirPackage, includes: "**/*.java")
      arg(line: additionalArgs.get())

      if (catalogFile.isPresent()) {
        depends(file: catalogFile.get().asFile)
      }
      bindingsFiles.each {
        binding(file: it.path)
      }
    }
  }
}
