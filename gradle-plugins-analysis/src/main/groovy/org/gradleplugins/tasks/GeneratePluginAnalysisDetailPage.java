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
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradleplugins.AnalyzeReport;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

public class GeneratePluginAnalysisDetailPage extends DefaultTask {
    private final RegularFileProperty report = newInputFile();
    private final RegularFileProperty detailHtml = newOutputFile();

    @InputFile
    public RegularFileProperty getReport() {
        return report;
    }

    @OutputFile
    public RegularFileProperty getDetailHtml() {
        return detailHtml;
    }

    @TaskAction
    private void doGenerateDetail() {
        AnalyzeReport r = null;
        String prettyJSONString = null;
        try (InputStream inStream = new FileInputStream(report.get().getAsFile())) {
            prettyJSONString = IOUtils.toString(inStream, Charset.defaultCharset());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Yaml yaml = new Yaml();

        Map<String,Object> map = (Map<String, Object>) yaml.load(prettyJSONString);

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(detailHtml.getAsFile().get())))) {
            out.println("---");
            out.println("layout: plugin");
            out.println(yaml.dump(map));
            out.println("---");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
