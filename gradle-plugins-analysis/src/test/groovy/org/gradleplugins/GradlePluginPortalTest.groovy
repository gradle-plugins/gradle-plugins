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

package org.gradleplugins

import spock.lang.Specification
import spock.lang.Subject

@Subject(GradlePluginPortal)
class GradlePluginPortalTest extends Specification {
    def "can page scrap the plugin information"() {
        def portal = GradlePluginPortal.connect(GradlePluginPortalTest.class.getResource("single/search.html"))

        expect:
        def plugins = portal.allPluginInformations

        plugins.size() == 10
        plugins*.pluginId == ["bue.akhikhl.wuff.eclipse-bundle", "com.polidea.cockpit", "net.wooga.build-unity", "org.ysb33r.os", "com.rapidminer.code-quality", "com.pluribuslabs.gradle.artifactversion", "us.ascendtech.js.npm", "us.ascendtech.js.gwt", "com.liferay.source.formatter", "nebula.test-jar"]
        plugins*.version == ["0.0.3", "1.0.3", "0.3.0", "0.9", "0.4.5-SNAPSHOT", "1.1.0", "0.1.4", "0.1.4", "2.3.188", "8.0.0"]
        plugins*.notation == ["gradle.plugin.bue.akhikhl.wuff:wuff-plugin:0.0.3", "gradle.plugin.com.polidea.cockpit:CockpitPlugin:1.0.3", "gradle.plugin.net.wooga.gradle:atlas-build-unity:0.3.0", "gradle.plugin.org.ysb33r.gradle:operatingsystem-gradle-plugin:0.9", "gradle.plugin.com.rapidminer.gradle:gradle-plugin-rapidminer-code-quality:0.4.5-SNAPSHOT", "gradle.plugin.com.pluribuslabs:artifact-version-plugin:1.1.0", "gradle.plugin.us.ascendtech:gwt-gradle:0.1.4", "gradle.plugin.us.ascendtech:gwt-gradle:0.1.4", "gradle.plugin.com.liferay:gradle-plugins-source-formatter:2.3.188", "com.netflix.nebula:nebula-publishing-plugin:8.0.0"]
    }

    def "can scrap multiple pages"() {
        def portal = GradlePluginPortal.connect(GradlePluginPortalTest.class.getResource("multi/search.html"))

        expect:
        portal.allPluginInformations.size() == 30
    }
}
