package org.gradleplugins;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeReport {
    private final String pluginId;
    private final boolean isJarAvailable;
    private final List<AnalyzeViolation> violations = new ArrayList<>();

    public AnalyzeReport(String pluginId) {
        this(pluginId, true);
    }

    private AnalyzeReport(String pluginId, boolean isJarAvailable) {
        this.pluginId = pluginId;
        this.isJarAvailable = isJarAvailable;
    }

    public String getPluginId() {
        return pluginId;
    }

    public boolean isJarAvailable() {
        return isJarAvailable;
    }

    public List<AnalyzeViolation> getViolations() {
        return violations;
    }

    public static AnalyzeReport noJarResolved(String pluginId) {
        return new AnalyzeReport(pluginId, false);
    }
}
