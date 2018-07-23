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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class GradlePluginPortalJustPluginId {
//    private static final Logger LOGGER = LogManager.getLogger(GradlePluginPortal.class);
    private final URL portalUrl;
    private int assumingPageCount = 0;

    GradlePluginPortalJustPluginId(URL portalUrl) {
        this.portalUrl = portalUrl;
    }

    public static GradlePluginPortalJustPluginId connect(URL pluginPortalUrl) {
        return new GradlePluginPortalJustPluginId(pluginPortalUrl);
    }

    public GradlePluginPortalJustPluginId assumingPageCount(int assumingPageCount) {
        this.assumingPageCount = assumingPageCount;
        return this;
    }

    public Set<ReleasedPluginInformation> getAllPluginInformations() {
        ExecutorService executor = null;
        List<Future<Set<ReleasedPluginInformation>>> futures = new ArrayList<>();
        if (assumingPageCount > 0) {
            executor = Executors.newFixedThreadPool(20);
            for (int i = 0; i <= assumingPageCount; ++i) {
                final int page = i;
                futures.add(executor.submit(() -> {
                    return get(page).getPlugins();
                }));
            }
        }

        Set<ReleasedPluginInformation> pluginInfos = new HashSet<>();
        Document doc;
        String value = "";
        if (assumingPageCount > 0) {
            value = "/search?page=" + assumingPageCount;
        }

        for (SearchPage page = new SearchPage(url(value)); page.hasMoreSearchPage(); page = page.getNextSearchPage()) {


            pluginInfos.addAll(page.getPlugins());
//            doc = fetch(url(value));
//
//            Elements e = doc.select("#search-results > tbody > tr");
//
//            if (!hasPlugins(e)) {
//                break;
//            }
//
//            pluginInfos.addAll(scrapPlugins(e));

//        } while (!(value = hasMorePage(doc)).isEmpty());
        }

        Set<ReleasedPluginInformation> result = new HashSet<>();
        for (Future<Set<ReleasedPluginInformation>> f : futures) {
            try {
                result.addAll(f.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (executor != null) {
            executor.shutdown();
        }

        result.addAll(pluginInfos);


        return result;
    }

    private Set<ReleasedPluginInformation> fetch(int page) {
        Document doc;
        Set<ReleasedPluginInformation> pluginInfos = new HashSet<>();

        doc = fetch(url(page));

        Elements e = doc.select("#search-results > tbody > tr");

        if (!hasPlugins(e)) {
            return Collections.emptySet();
        }

        pluginInfos.addAll(scrapPlugins(e));

        return pluginInfos;
    }

    private static boolean hasPlugins(Elements e) {
        Elements noPlugin = e.first().select("td > em");
        if (noPlugin.size() == 1) {
            assert noPlugin.first().text() == "No plugins found.";
            return false;
        }
        return true;
    }

    private Set<ReleasedPluginInformation> scrapPlugins(Elements e) {
        Set<ReleasedPluginInformation> result = new HashSet<>();
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
            result.add(pluginInfo);
        }

        return result;
    }

    private String getRootUrl() {
        int g = portalUrl.toString().lastIndexOf('/');
        return portalUrl.toString().substring(0, g);
    }

    private URL url(String path) {
        try {
            return new URL(getRootUrl() + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private URL url(int page) {
        try {
            return new URL(getRootUrl() + "/search?page=" + page);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Document fetch(URL url) {
        try {
//            System.out.println("Fetching " + url.toString());
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

    private SearchPage get(int pageNumber) {
        return new SearchPage(url(pageNumber));
    }

//    private class SearchPageChainingIterator implements Iterator<SearchPage> {
//
//
//        @Override
//        public boolean hasNext() {
//            return false;
//        }
//
//        @Override
//        public SearchPage next() {
//            return null;
//        }
//
//        @Override
//        public void remove() {
//
//        }
//    }
//
//    private class AssumingSearchPageIterator implements Iterator<SearchPage> {
//        private int pageNumber = -1;
//        private SearchPage lastSearchPage = null;
//
//        @Override
//        public boolean hasNext() {
//            if (lastSearchPage != null) {
//                return lastSearchPage.hasMoreSearchPage();
//            }
//            return true;
//        }
//
//        @Override
//        public SearchPage next() {
//            return lastSearchPage = new SearchPage(url(pageNumber++));
//        }
//
//        @Override
//        public void remove() {
//            throw new UnsupportedOperationException();
//        }
//    }

    private class SearchPage {
        private final URL searchPageUrl;
        private Document searchPageDocument;
        private Set<ReleasedPluginInformation> scrappedPlugins;

        public SearchPage(URL searchPageUrl) {
            this.searchPageUrl = searchPageUrl;
        }

        public Set<ReleasedPluginInformation> getPlugins() {
            if (scrappedPlugins == null) {
                Elements e = getSearchPageDocument().select("#search-results > tbody > tr");

                if (hasPlugins(e)) {
                    scrappedPlugins = scrapPlugins(e);
                } else {
                    scrappedPlugins = Collections.emptySet();
                }
            }

            return scrappedPlugins;
        }

        public boolean hasMoreSearchPage() {
            return hasMorePage(getSearchPageDocument()).length() > 0;
        }

        public SearchPage getNextSearchPage() {
            return new SearchPage(url(hasMorePage(getSearchPageDocument())));
        }

        private Document getSearchPageDocument() {
            if (searchPageDocument == null) {
                searchPageDocument = fetch(searchPageUrl);
            }
            return searchPageDocument;
        }
    }
}
