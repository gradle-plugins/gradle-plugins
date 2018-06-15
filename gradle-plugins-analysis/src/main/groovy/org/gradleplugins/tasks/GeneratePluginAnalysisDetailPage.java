package org.gradleplugins.tasks;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradleplugins.AnalyzeReport;

import java.io.*;
import java.nio.charset.Charset;

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
        Gson gson = new Gson();
        AnalyzeReport r = null;
        try (InputStream inStream = new FileInputStream(report.get().getAsFile())) {
            r = gson.fromJson(IOUtils.toString(inStream, Charset.defaultCharset()), AnalyzeReport.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(detailHtml.getAsFile().get())))) {
            out.println("<html>");
            out.println("<body>");
            out.println("<h1>" + r.getPluginId() + "</h1>");
            out.println("<h2>There are " + r.getViolations().size() + " violation" + (r.getViolations().size() == 1 ? "" : "s") + "</h2>");
            out.println("</body>");
            out.println("</html>");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
