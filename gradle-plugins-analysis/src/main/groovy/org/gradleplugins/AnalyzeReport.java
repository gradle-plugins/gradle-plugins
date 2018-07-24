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

package org.gradleplugins;

import java.util.HashSet;
import java.util.Set;

public class AnalyzeReport {
    private final String pluginId;
    private final String jarSha1;
    private final boolean isJarAvailable;
    private final String error;
    private final Set<AnalyzeViolation> violations = new HashSet<>();

    public AnalyzeReport(String pluginId, String jarSha1) {
        this(pluginId, jarSha1,true);
    }

    private AnalyzeReport(String pluginId, String jarSha1, boolean isJarAvailable) {
        this(pluginId, jarSha1, isJarAvailable, "NONE");
    }

    private AnalyzeReport(String pluginId, String jarSha1, boolean isJarAvailable, String error) {
        this.pluginId = pluginId;
        this.jarSha1 = jarSha1;
        this.isJarAvailable = isJarAvailable;
        this.error = error;
    }

    public String getPluginId() {
        return pluginId;
    }

    public boolean isJarAvailable() {
        return isJarAvailable;
    }

    public String getError() {
        return error;
    }

    public Set<AnalyzeViolation> getViolations() {
        return violations;
    }

    public static AnalyzeReport noJarResolved(String pluginId) {
        return new AnalyzeReport(pluginId, "INVALID", false);
    }

    public AnalyzeReport toAnalysisError(Throwable throwable) {
        AnalyzeReport result = new AnalyzeReport(pluginId, jarSha1, isJarAvailable, throwable.getMessage());
        result.getViolations().addAll(getViolations());
        return result;
    }
}
