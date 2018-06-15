package org.gradleplugins;

import org.gradle.internal.impldep.com.google.common.base.Objects;

import java.net.URI;
import java.net.URL;

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
    public String toString() {
        return Objects.toStringHelper(ReleasedPluginInformation.class)
                .add("pluginId", pluginId)
                .add("portalUrl", portalUrl)
                .add("description", description)
                .add("latestVersion", latestVersion)
                .add("notation", notation)
                .toString();
    }
}
