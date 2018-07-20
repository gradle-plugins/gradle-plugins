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

package org.gradleplugins.tasks;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

public class CloneWebsite extends DefaultTask {
    private final Property<String> repositoryUri = getProject().getObjects().property(String.class);
    private final DirectoryProperty repositoryDirectory = newOutputDirectory();

    public DirectoryProperty getRepositoryDirectory() {
        return repositoryDirectory;
    }

    public Property<String> getRepositoryUri() {
        return repositoryUri;
    }

    @TaskAction
    private void doSync() {
        try {
            FileUtils.deleteDirectory(repositoryDirectory.get().getAsFile());
            Git git = Git.cloneRepository()
                    .setURI(repositoryUri.get())
                    .setDirectory(repositoryDirectory.get().getAsFile())
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
