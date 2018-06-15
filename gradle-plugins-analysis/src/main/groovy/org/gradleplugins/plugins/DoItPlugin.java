package org.gradleplugins.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradleplugins.tasks.ScrapPluginPortal;

import java.net.MalformedURLException;
import java.net.URL;

public class DoItPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().create("scrap", ScrapPluginPortal.class, (it) -> {
            try {
                it.getPluginPortalUrl().set(new URL("https://plugins.gradle.org/"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            it.getPluginData().set(project.getLayout().getBuildDirectory().file("scrap.json"));
        });
    }
}
