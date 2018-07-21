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

import java.net.URI;
import java.net.URL;
import java.util.Objects;

public class ReleasedPluginInformation {
    private final String pluginId;
    private final URL portalUrl;
    private final String description;
    private final String latestVersion;
    private final String notation;

    public ReleasedPluginInformation(String pluginId, URL portalUrl, String description, String latestVersion, String notation) {
        this.pluginId = pluginId;
        this.portalUrl = portalUrl;
        this.description = description;
        this.latestVersion = latestVersion;
        this.notation = notation;
    }

    public String getPluginId() {
        return pluginId;
    }

    public URL getPortalUrl() {
        return portalUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getNotation() {
        return notation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReleasedPluginInformation that = (ReleasedPluginInformation) o;
        return Objects.equals(pluginId, that.pluginId) &&
            Objects.equals(portalUrl, that.portalUrl) &&
            Objects.equals(description, that.description) &&
            Objects.equals(latestVersion, that.latestVersion) &&
            Objects.equals(notation, that.notation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, portalUrl, description, latestVersion, notation);
    }

//    @Override
//    public String toString() {
//        return Objects.toStringHelper(ReleasedPluginInformation.class)
//                .add("pluginId", pluginId)
//                .add("portalUrl", portalUrl)
//                .add("description", description)
//                .add("latestVersion", latestVersion)
//                .add("notation", notation)
//                .toString();
//    }
}
