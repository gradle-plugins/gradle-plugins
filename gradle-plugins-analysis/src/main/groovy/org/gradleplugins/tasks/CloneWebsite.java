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
