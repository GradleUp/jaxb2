package com.gradleup.jaxb

import com.gradleup.jaxb.tasks.CleanJaxb2SourcesDir
import com.gradleup.jaxb.tasks.GenerateJaxb2Classes
import com.gradleup.jaxb.tasks.InitJaxb2SourcesDir
import com.gradleup.jaxb.tasks.XjcTaskConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.internal.reflect.Instantiator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A plugin for generating Java source code from XSD files backed by JAXB2 ant task.
 *
 * @author holgerstolzenberg
 * @since 1.0.0
 */
class Jaxb2Plugin implements Plugin<Project> {
  public static final String TASK_GROUP = "Jaxb2"

  private static final Logger LOG = LoggerFactory.getLogger(Jaxb2Plugin.class)

  private static final String SLF4J_VERSION = '1.7.13'
  private static final String JAXB2_VERSION = '0.9.5'
  private static final String JAXB_VERSION = '2.2.11'
  private static final String COMMONS_LOGGING_VERSION = '1.2'
  private static final String JAXB_OUTPUT_DIR_BASE = "gradleup/jaxb2"

  @Override
  void apply(final Project project) {
    // do nothing if plugin is already applied
    if (project.plugins.hasPlugin(Jaxb2Plugin.class)) {
      return
    }

    LOG.info("Applying JAXB2 plugin")

    applyJavaPlugin(project)
    addJaxb2Extension(project)
    addJaxb2Configuration(project)
    addTasks(project)
    addJaxbDependencies(project)

    // make sure project configuration has finished
    project.afterEvaluate {
      addSourceSet(project)
    }
  }

  private static Configuration addJaxb2Configuration(Project project) {
    project.configurations.maybeCreate('jaxb2')
  }

  private static void addJaxb2Extension(Project project) {
    project.extensions.create(Jaxb2PluginExtension.NAME, Jaxb2PluginExtension)
    project.extensions.jaxb2.extensions.xjc = project.container(XjcTaskConfig) { String name ->
      return project.gradle.services.get(Instantiator).newInstance(XjcTaskConfig, name)
    }
  }

  private static void applyJavaPlugin(Project project) {
    // apply core 'java' plugin if not present to make 'sourceSets' available
    if (!project.plugins.hasPlugin(JavaPlugin.class)) {
      project.plugins.apply(JavaPlugin.class)
    }
  }

  private static void addTasks(Project project) {
    // add new tasks for creating/cleaning the auto-value sources dir
    project.task(type: CleanJaxb2SourcesDir, "cleanJaxb2SourcesDir")
    project.task(type: InitJaxb2SourcesDir, "initJaxb2SourcesDir")

    project.tasks.clean.dependsOn project.tasks.cleanJaxb2SourcesDir

    project.extensions.jaxb2.xjc.all { registerGenerateJaxb2ClassesTask(project, it) }
  }

  private static void registerGenerateJaxb2ClassesTask(Project project, XjcTaskConfig xjcConfig) {
    def generationTaskConfig = xjcConfig
    def generationTask = project.tasks.register("generateJaxb2Classes${generationTaskConfig.name.capitalize()}", GenerateJaxb2Classes) {

      it.generatedSourcesDirectory.set(project.layout.buildDirectory.dir("$JAXB_OUTPUT_DIR_BASE/${generationTaskConfig.name}"))
      it.schemaFile.set(project.layout.projectDirectory.file(generationTaskConfig.schema))
      it.basePackage.set(generationTaskConfig.basePackage)
      it.encoding.set(generationTaskConfig.encoding)
      it.extension.set(generationTaskConfig.extension)
      it.additionalArgs.set(generationTaskConfig.additionalArgs)
      it.header.set(generationTaskConfig.header)

      if (generationTaskConfig.catalog != null) {
        it.catalogFile.set(project.layout.projectDirectory.file(generationTaskConfig.catalog))
      }

      it.bindingsFiles.from(collectBindingsFiles(project, generationTaskConfig.bindingsDir, generationTaskConfig.includedBindingFiles))

      dependsOn project.tasks.initJaxb2SourcesDir
    }

    def copyGeneratedClassesTask = project.tasks.register("copyJaxb2Classes${generationTaskConfig.name.capitalize()}", Copy) {
      def packagePathProvider = project.provider { generationTaskConfig.basePackage.replace(".", "/") }
      from packagePathProvider.map { packagePath -> project.layout.buildDirectory.dir("$JAXB_OUTPUT_DIR_BASE/${generationTaskConfig.name}/$packagePath") }
      into packagePathProvider.map { packagePath -> project.layout.projectDirectory.dir("${generationTaskConfig.generatedSourcesDir}/$packagePath") }
      dependsOn generationTask
    }

    project.tasks.compileJava.dependsOn copyGeneratedClassesTask
  }

  private static Set<File> collectBindingsFiles(Project project, String bindingsDir, String includedBindingFiles) {
    def bindingsFileTreeBaseDir = bindingsDir != null ? project.layout.projectDirectory.dir(bindingsDir) : project.layout.projectDirectory
    def bindingsFilesTree = project.fileTree(bindingsFileTreeBaseDir)
    if (includedBindingFiles != null) {
      includedBindingFiles.trim().split(", ").each {  bindingsFilesTree.include(it) }
    } else {
      bindingsFilesTree.include("**/*.xjb")
    }
    bindingsFilesTree.files
  }

  private addJaxbDependencies(Project project) {
    project.dependencies {
      jaxb2 'org.slf4j:slf4j-simple:' + SLF4J_VERSION

      jaxb2 'org.jvnet.jaxb2_commons:jaxb2-basics-runtime:' + JAXB2_VERSION
      jaxb2 'org.jvnet.jaxb2_commons:jaxb2-basics-ant:' + JAXB2_VERSION
      jaxb2 'org.jvnet.jaxb2_commons:jaxb2-basics:' + JAXB2_VERSION
      jaxb2 'org.jvnet.jaxb2_commons:jaxb2-basics-tools:' + JAXB2_VERSION
      jaxb2 'commons-logging:commons-logging:' + COMMONS_LOGGING_VERSION

      jaxb2 'com.sun.xml.bind:jaxb-core:' + JAXB_VERSION
      jaxb2 'com.sun.xml.bind:jaxb-xjc:' + JAXB_VERSION
      jaxb2 'com.sun.xml.bind:jaxb-impl:' + JAXB_VERSION
    }
  }

  private void addSourceSet(Project project) {
    Set<XjcTaskConfig> xjcConfigs = project.extensions.jaxb2.xjc

    for (XjcTaskConfig theConfig : xjcConfigs) {
      if (theConfig.createSourceSet) {
        File sourcesDir = project.file(theConfig.generatedSourcesDir)
        LOG.info("Create source set 'jaxb2': {}", sourcesDir.absolutePath)

        project.sourceSets {
          jaxb2 {
            java.srcDirs += [sourcesDir]
          }
        }

        project.compileJava {
          source += project.files(sourcesDir)
        }
      }
    }
  }
}
