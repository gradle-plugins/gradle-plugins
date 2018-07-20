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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradleplugins.ReleasedPluginInformation;
import org.gradleplugins.tasks.AnalyzeBytecode;
import org.gradleplugins.tasks.CloneWebsite;
import org.gradleplugins.tasks.Commit;
import org.gradleplugins.tasks.GeneratePluginAnalysisDetailPage;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PluginPortalAnalysisPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<ReleasedPluginInformation>>(){}.getType();
        try (InputStream inStream = new FileInputStream(project.file("scrap.json"))) {
            List<ReleasedPluginInformation> l = gson.fromJson(IOUtils.toString(inStream, Charset.defaultCharset()), listType);

            CloneWebsite clone = project.getTasks().create("cloneRepo", CloneWebsite.class, (it) -> {
                it.getRepositoryDirectory().set(project.getLayout().getBuildDirectory().dir("clone/repo"));
                it.getRepositoryUri().set("https://github.com/gradle-plugins/gradle-plugins.github.io.git");
            });

            int bucketCount = 5;

            int i = 0;
            for (ReleasedPluginInformation p : l) {
                i++;
                // TODO: Remove maybeCreate
                Configuration c = project.getConfigurations().maybeCreate(p.getPluginId());
                c.setTransitive(false);
                c.setCanBeResolved(true);
                c.setCanBeConsumed(false);

                project.getDependencies().add(c.getName(), p.getNotation());

                // TODO: Remove maybeCreate
                AnalyzeBytecode analyzeTask = project.getTasks().maybeCreate(p.getPluginId(), AnalyzeBytecode.class);
                analyzeTask.getJar().set(project.getLayout().file(project.provider(() -> {
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

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        project.getRepositories().gradlePluginPortal();
    }
}
