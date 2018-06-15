package org.gradleplugins;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeReport {
    private final String pluginId;
    private final List<AnalyzeViolation> violations = new ArrayList<>();

    public AnalyzeReport(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }

    public List<AnalyzeViolation> getViolations() {
        return violations;
    }
}
