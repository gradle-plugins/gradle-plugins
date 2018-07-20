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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public class Commit extends DefaultTask {
    private final DirectoryProperty repositoryDirectory = newInputDirectory();
    private final ConfigurableFileCollection source = getProject().files();
    private final Property<String> username = getProject().getObjects().property(String.class);
    private final Property<String> password = getProject().getObjects().property(String.class);

    public Commit() {
        dependsOn(source);
    }

    public DirectoryProperty getRepositoryDirectory() {
        return repositoryDirectory;
    }

    public ConfigurableFileCollection getSource() {
        return source;
    }

    public Property<String> getUsername() {
        return username;
    }

    public Property<String> getPassword() {
        return password;
    }

    @TaskAction
    private void doCommit() {
        // now open the resulting repository with a FileRepositoryBuilder
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
//            Repository repository = builder.setGitDir(repositoryDirectory.get().getAsFile())
//                    .readEnvironment() // scan environment GIT_* variables
//                    .findGitDir() // scan up the file system tree
//                    .build();
//            System.out.println("Having repository: " + repository.getDirectory());

            try {
                Git git = Git.open(repositoryDirectory.get().getAsFile());

                // run the add
                for (File f : source) {
//                    System.out.println(f.getAbsolutePath());
//                    System.out.println(repositoryDirectory.getAsFile().get().getAbsolutePath());
//                    System.out.println(f.getAbsolutePath().replace(repositoryDirectory.getAsFile().get().getAbsolutePath() + "/", ""));
                    git.add().addFilepattern(f.getAbsolutePath().replace(repositoryDirectory.getAsFile().get().getAbsolutePath() + "/", "")).call();
                }

                // and then commit the changes
                git.commit().setAuthor("Daniel Lacasse", "daniel@lacasse.io").setMessage("Added testfile").call();

                git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username.get(), password.get())).setRebase(true).call();
                git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username.get(), password.get())).call();

            } catch (UnmergedPathsException e) {
                e.printStackTrace();
            } catch (WrongRepositoryStateException e) {
                e.printStackTrace();
            } catch (ConcurrentRefUpdateException e) {
                e.printStackTrace();
            } catch (NoFilepatternException e) {
                e.printStackTrace();
            } catch (NoHeadException e) {
                e.printStackTrace();
            } catch (NoMessageException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
