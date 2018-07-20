/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
