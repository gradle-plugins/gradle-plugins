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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.LoggingManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GradlePluginPortal {
//    private static final Logger LOGGER = LogManager.getLogger(GradlePluginPortal.class);
    private final URL pluginPortalUrl;

    GradlePluginPortal(URL pluginPortalUrl) {
        this.pluginPortalUrl = pluginPortalUrl;
    }

    public static GradlePluginPortal connect(URL pluginPortalUrl) {
        return new GradlePluginPortal(pluginPortalUrl);
    }

    public List<ReleasedPluginInformation> getAllPluginInformations() {
        List<ReleasedPluginInformation> pluginInfos = new ArrayList<>();
        Document doc;
        String value = "";
        do {
            if (value.isEmpty()) {
                doc = fetch(pluginPortalUrl);
            } else {
                doc = fetch(url(value));
            }

            Elements e = doc.select("#search-results > tbody > tr");

            for (Element it : e) {
                Elements g = it.select("td > h3 > a");
                assert g.size() == 1;
                URL portalUrl = url(g.first().attr("href"));
                String pluginId = g.first().text();

                Elements h = it.select("td > p");
                assert h.size() == 1;
                String description = h.first().text();

                Elements j = it.select("td[class=version] > span[class=latest-version]");
                assert j.size() == 1;
                String latestVersion = j.first().text();


                Document plugDoc = fetch(portalUrl);
                Elements k = plugDoc.select("div[class=use] > pre > code[class=language-groovy]");
                assert k.size() == 2;
                String notation = "";
                for (String s : k.get(1).text().split("\n")) {
                    if (s.trim().startsWith("classpath")) {
                        notation = s.trim().substring(11, s.trim().length() - 1);
                        break;
                    }
                }

                ReleasedPluginInformation pluginInfo = new ReleasedPluginInformation(pluginId, portalUrl, description, latestVersion, notation);
                pluginInfos.add(pluginInfo);
            }
        } while (!(value = hasMorePage(doc)).isEmpty());

        return pluginInfos;
    }

    private String getRootUrl() {
        int g = pluginPortalUrl.toString().lastIndexOf('/');
        return pluginPortalUrl.toString().substring(0, g);
    }

    private URL url(String path) {
        try {
            return new URL(getRootUrl() + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Document fetch(URL url) {
        try {
            System.out.println("Fetching " + url.toString());
            return Jsoup.parse(new String(IOUtils.toByteArray(url)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String hasMorePage(Document doc) {
        Elements g = doc.select("div[class=page-link clearfix] > a");
        if (g.size() == 0) {
            return "";
        } else if (g.size() == 2) {
            return g.get(1).attr("href");
        } else {
            assert g.size() == 1;
            if (g.first().text().equals("Next")) {
                return g.first().attr("href");
            }
            return "";
        }
    }
}
