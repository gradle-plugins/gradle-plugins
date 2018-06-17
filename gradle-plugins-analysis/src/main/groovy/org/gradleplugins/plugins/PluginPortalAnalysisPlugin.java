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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class PluginPortalAnalysisPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<ReleasedPluginInformation>>(){}.getType();
        try (InputStream inStream = new FileInputStream(project.file("scrap.json"))) {
            List<ReleasedPluginInformation> l = gson.fromJson(IOUtils.toString(inStream, Charset.defaultCharset()), listType);

            Task analyzeAllTask = project.getTasks().create("analyze");

            CloneWebsite clone = project.getTasks().create("cloneRepo", CloneWebsite.class, (it) -> {
                it.getRepositoryDirectory().set(project.getLayout().getBuildDirectory().dir("clone/repo"));
                it.getRepositoryUri().set("https://github.com/gradle-plugins/gradle-plugins.github.io.git");
            });

            Commit commit = project.getTasks().create("commit", Commit.class, (it) -> {
                it.getRepositoryDirectory().set(clone.getRepositoryDirectory());
                it.getUsername().set(System.getenv("USERNAME"));
                it.getPassword().set(System.getenv("PASSWORD"));
            });

            for (ReleasedPluginInformation p : l.subList(0, 3)) {
                Configuration c = project.getConfigurations().maybeCreate(p.getPluginId());
                c.setTransitive(false);
                c.setCanBeResolved(true);
                c.setCanBeConsumed(false);

                project.getDependencies().add(c.getName(), p.getNotation());

                AnalyzeBytecode analyzeTask = project.getTasks().maybeCreate(p.getPluginId(), AnalyzeBytecode.class);
                analyzeTask.getJar().set(project.getLayout().file(project.provider(() -> c.getSingleFile())));
                analyzeTask.getPluginId().set(p.getPluginId());
                analyzeTask.getReport().set(project.getLayout().getBuildDirectory().file("analysisReport/" + p.getPluginId() + ".json"));

                analyzeAllTask.dependsOn(analyzeTask);

                GeneratePluginAnalysisDetailPage analysisDetailPage = project.getTasks().maybeCreate("generate" + p.getPluginId(), GeneratePluginAnalysisDetailPage.class);
                analysisDetailPage.getDetailHtml().set(clone.getRepositoryDirectory().file("plugins/" + p.getPluginId() + ".md"));
                analysisDetailPage.getReport().set(analyzeTask.getReport());

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
