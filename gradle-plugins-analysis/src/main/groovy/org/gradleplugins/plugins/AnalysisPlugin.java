package org.gradleplugins.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradleplugins.tasks.AnalyzeBytecode;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class AnalysisPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().create("analyzeBytecode", AnalyzeBytecode.class, (AnalyzeBytecode task) -> {
            task.getJar().set(project.file("gradle-plugins-service-builder-2.1.14.jar"));
        });
    }
}