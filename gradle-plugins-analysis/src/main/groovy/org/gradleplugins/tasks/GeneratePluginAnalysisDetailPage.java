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
