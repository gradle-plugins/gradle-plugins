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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
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

            for (ReleasedPluginInformation p : l) {
                Configuration c = project.getConfigurations().maybeCreate(p.getPluginId());
                c.setTransitive(false);
                c.setCanBeResolved(true);
                c.setCanBeConsumed(false);

                project.getDependencies().add(c.getName(), p.getNotation());

                AnalyzeBytecode analyzeTask = project.getTasks().maybeCreate(p.getPluginId(), AnalyzeBytecode.class);
                analyzeTask.getJar().set(project.getLayout().file(project.provider(() -> c.getSingleFile())));
                analyzeAllTask.dependsOn(analyzeTask);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        project.getRepositories().gradlePluginPortal();
    }
}
