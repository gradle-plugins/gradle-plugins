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
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradleplugins.GradlePluginPortalJustPluginId;
import org.gradleplugins.ReleasedPluginInformation;
import org.gradleplugins.tasks.AnalyzeBytecode;
import org.gradleplugins.tasks.CloneWebsite;
import org.gradleplugins.tasks.Commit;
import org.gradleplugins.tasks.GeneratePluginAnalysisDetailPage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

public class PluginPortalAnalysisPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        try {
            CloneWebsite clone = project.getTasks().create("cloneRepo", CloneWebsite.class, (it) -> {
                it.getRepositoryDirectory().set(project.getLayout().getBuildDirectory().dir("clone/repo"));
                it.getRepositoryUri().set("https://github.com/gradle-plugins/gradle-plugins.github.io.git");
            });

            int bucketCount = 1;

            int i = 0;
            for (ReleasedPluginInformation p : GradlePluginPortalJustPluginId.connect(new URL("https://plugins.gradle.org/")).getAllPluginInformations()) {
                i++;
                // TODO: Remove maybeCreate
                AnalyzeBytecode analyzeTask = project.getTasks().maybeCreate(p.getPluginId(), AnalyzeBytecode.class);
                analyzeTask.getJar().set(project.getLayout().file(project.provider(() -> {
                    // TODO: Remove maybeCreate
                    Configuration c = project.getConfigurations().findByName(p.getPluginId());
                    if (c == null) {
                        c = project.getConfigurations().maybeCreate(p.getPluginId());
                        c.setTransitive(false);
                        c.setCanBeResolved(true);
                        c.setCanBeConsumed(false);

                        project.getDependencies().add(c.getName(), p.getNotation());
                    }

                    Set<File> files = c.getResolvedConfiguration().getLenientConfiguration().getFiles();
                    if (files.isEmpty()) {
                        return null;
                    }

                    assert files.size() == 1;
                    return files.iterator().next();
                })));
                analyzeTask.getPluginId().set(p.getPluginId());
                analyzeTask.getReport().set(project.getLayout().getBuildDirectory().file("analysisReport/" + p.getPluginId() + ".json"));

                Task bucket = project.getTasks().maybeCreate("analyzeBucket" + (i % bucketCount));
                bucket.dependsOn(analyzeTask);


                // TODO: Remove maybeCreate
                GeneratePluginAnalysisDetailPage analysisDetailPage = project.getTasks().maybeCreate("generate" + p.getPluginId(), GeneratePluginAnalysisDetailPage.class);
                analysisDetailPage.getDetailHtml().set(clone.getRepositoryDirectory().file("plugins/" + p.getPluginId() + ".md"));
                analysisDetailPage.getReport().set(analyzeTask.getReport());


                Commit commit = project.getTasks().maybeCreate("commitBucket" + (i % bucketCount), Commit.class);
                commit.getRepositoryDirectory().set(clone.getRepositoryDirectory());
                commit.getUsername().set(System.getenv("USERNAME"));
                commit.getPassword().set(System.getenv("PASSWORD"));
                commit.getSource().from(analysisDetailPage.getDetailHtml());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        project.getRepositories().gradlePluginPortal();
    }
}
