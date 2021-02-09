package com.gradleup.jaxb

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class PluginTest extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()
    File projectDir

    def setup() {
        projectDir = temporaryFolder.root.toPath().toRealPath().toFile()
    }

    def 'generate files'() {
        given:
        copyResources("project")

        when:
        def result1 = build("generateJaxb2Classes")

        then:
        result1.task(":generateJaxb2Classes").outcome == TaskOutcome.SUCCESS
    }

    def 'compile project'() {
        given:
        copyResources("project")

        when:
        def result1 = build("assemble")

        then:
        result1.task(":generateJaxb2Classes").outcome == TaskOutcome.SUCCESS
    }

    protected final GradleRunner newRunner(final String... args) {
        List<String> additionalArgs = ["--warning-mode=fail"]
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments([*args, *additionalArgs])
            .withPluginClasspath()
            .forwardOutput()
    }

    protected final BuildResult build(final String... args) {
        return newRunner(args).build()
    }

    protected void copyResources(String source, String destination = "") {
        ClassLoader classLoader = getClass().getClassLoader()
        URL resource = classLoader.getResource(source)
        if (resource == null) {
            throw new RuntimeException("Could not find classpath resource: $source")
        }

        File resourceFile = new File(resource.toURI())
        if (resourceFile.file) {
            File destinationFile = file(destination)
            FileUtils.copyFile(resourceFile, destinationFile)
        } else {
            def destinationDir = directory(destination)
            FileUtils.copyDirectory(resourceFile, destinationDir)
        }
    }

    protected final File file(String path, File baseDir = getProjectDir()) {
        def splitted = path.split('/')
        def directory = splitted.size() > 1 ? directory(splitted[0..-2].join('/'), baseDir) : baseDir
        def file = new File(directory, splitted[-1])
        file.createNewFile()
        return file
    }

    protected final File directory(String path, File baseDir = getProjectDir()) {
        return new File(baseDir, path).with {
            mkdirs()
            it
        }
    }
}
