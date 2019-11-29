package com.ewerk.gradle.plugins;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.Assert.assertEquals;

public class IntegrationTest {

    @Test
    public void testInitJaxb2SourcesDir() {
        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(loadFile("testKit"))
                .withArguments("initJaxb2SourcesDir")
                .build();

        assertEquals(SUCCESS, result.task(":initJaxb2SourcesDir").getOutcome());
    }

    @Test
    @Ignore("Fails intermittently")
    public void testGenerateJaxb2Classes() {
        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(loadFile("testKit"))
                .withArguments("generateJaxb2Classes")
                .build();

        assertEquals(SUCCESS, result.task(":initJaxb2SourcesDir").getOutcome());
        assertEquals(SUCCESS, result.task(":generateJaxb2Classes").getOutcome());
    }

    @Test
    public void testCleanJaxb2SourcesDir() {
        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(loadFile("testKit"))
                .withArguments("cleanJaxb2SourcesDir")
                .build();

        assertEquals(SUCCESS, result.task(":cleanJaxb2SourcesDir").getOutcome());
    }

    private File loadFile(String filename) {
        URL resource = getClass().getClassLoader().getResource(filename);

        if (resource != null) {
            return new File(resource.getFile());
        } else {
            return null;
        }
    }

}
