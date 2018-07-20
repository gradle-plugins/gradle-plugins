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
