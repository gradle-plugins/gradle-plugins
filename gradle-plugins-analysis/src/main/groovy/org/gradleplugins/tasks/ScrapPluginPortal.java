package org.gradleplugins.tasks;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradleplugins.GradlePluginPortal;
import org.gradleplugins.ReleasedPluginInformation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

public class ScrapPluginPortal extends DefaultTask {
    private final Property<URL> pluginPortalUrl = getProject().getObjects().property(URL.class);
    private final RegularFileProperty pluginData = newOutputFile();

    @TaskAction
    private void doScrapping() {
        Gson gson = new Gson();
        List<ReleasedPluginInformation> plugins = GradlePluginPortal.connect(pluginPortalUrl.get()).getAllPluginInformations();

        try (OutputStream outStream = new FileOutputStream(pluginData.getAsFile().get())) {
            IOUtils.write(gson.toJson(plugins), outStream, Charset.defaultCharset());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Input
    public Property<URL> getPluginPortalUrl() {
        return pluginPortalUrl;
    }

    @OutputFile
    public RegularFileProperty getPluginData() {
        return pluginData;
    }
}
