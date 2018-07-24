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

import org.apache.commons.io.IOUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.gradleplugins.GradlePluginPortal;
import org.gradleplugins.ReleasedPluginInformation;
import org.gradleplugins.tasks.AnalyzeBytecode;
import org.gradleplugins.tasks.CloneWebsite;
import org.gradleplugins.tasks.Commit;
import org.gradleplugins.tasks.GeneratePluginAnalysisDetailPage;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.Callable;

public class PluginPortalAnalysisPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        try {
            TaskProvider<CloneWebsite> clone = project.getTasks().register("cloneRepo", CloneWebsite.class, (it) -> {
                it.getRepositoryDirectory().set(project.getLayout().getBuildDirectory().dir("clone/repo"));
                it.getRepositoryUri().set("https://github.com/gradle-plugins/gradle-plugins.github.io.git");
            });

            TaskProvider<Commit> commit = project.getTasks().register("commitBucket0", Commit.class, (it) -> {
                it.getRepositoryDirectory().set(clone.get().getRepositoryDirectory());
                it.getUsername().set(System.getenv("USERNAME"));
                it.getPassword().set(System.getenv("PASSWORD"));
            });

            TaskProvider<Task> bucket = project.getTasks().register("analyzeBucket0");

            for (ReleasedPluginInformation p : GradlePluginPortal.connect(new URL("https://plugins.gradle.org/")).assumingPageCount(260).withCache(project.file("plugins.cache")).withGradleOffline(project.getGradle().getStartParameter().isOffline()).getAllPluginInformations()) {
                ReleasedPluginInformation pp = p;
                TaskProvider<AnalyzeBytecode> analyzeTask = project.getTasks().register(p.getPluginId() + "_" + p.getVersion(), AnalyzeBytecode.class, (it) -> {
                    it.getJarSha1().set(project.provider(new Callable<String>() {
                        private String cache;

                        @Override
                        public String call() throws Exception {
                            if (cache != null) {
                                return cache;
                            }

                            Configuration c = project.getConfigurations().findByName(pp.getPluginId() + "sha");
                            if (c == null) {
                                c = project.getConfigurations().create(pp.getPluginId() + "sha");
                                c.setTransitive(false);
                                c.setCanBeResolved(true);
                                c.setCanBeConsumed(false);

                                project.getDependencies().add(c.getName(), pp.getNotation() + "@jar.sha1");
                            }

                            Set<File> files = c.getResolvedConfiguration().getLenientConfiguration().getFiles();
                            if (files.isEmpty()) {
                                return cache = "INVALID";
                            }

                            assert files.size() == 1;
                            try (InputStream inStream = new FileInputStream(files.iterator().next())) {
                                return cache = IOUtils.toString(inStream, Charset.defaultCharset());
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return cache = "INVALID";
                        }
                    }));
                    // TODO: Turn this on, once all plugins has checksum for plugin and for analysis
//                    it.setEnabled(((Spec<Task>) task -> {
//                        return !it.getJarSha1().get().equals("0e822d1c6c796253a3cf23e77e3a7568ec7c09eb");
//                    }).isSatisfiedBy(it));
                    it.getJar().set(project.getLayout().file(project.provider(() -> {
                        Configuration c = project.getConfigurations().findByName(p.getPluginId());
                        if (c == null) {
                            c = project.getConfigurations().create(p.getPluginId());
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
                    it.getPluginId().set(p.getPluginId());
                    it.getReport().set(project.getLayout().getBuildDirectory().file("analysisReport/" + p.getPluginId() + "_" + p.getVersion() + ".json"));
                });

                bucket.configure((it) -> {
                    it.dependsOn(analyzeTask);
                });


                TaskProvider<GeneratePluginAnalysisDetailPage> analysisDetailPage = project.getTasks().register("generate" + p.getPluginId() + "_" + p.getVersion(), GeneratePluginAnalysisDetailPage.class, (it) -> {
                    it.getDetailHtml().set(clone.get().getRepositoryDirectory().file("plugins/" + p.getPluginId() + "/" + p.getVersion() + ".md"));
                    it.getReport().set(analyzeTask.get().getReport());
                });


                commit.configure((it) -> {
                    it.getSource().from(analysisDetailPage.get().getDetailHtml());
                });
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        project.getRepositories().gradlePluginPortal();
    }
}
