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

import com.google.gson.Gson;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;
import org.gradleplugins.AnalyzeReport;
import org.gradleplugins.AnalyzeViolation;
import org.gradleplugins.InternalApiUsageViolation;
import org.objectweb.asm.*;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.Charset;

public class AnalyzeBytecode extends DefaultTask {
    private final Property<String> pluginId = getProject().getObjects().property(String.class);
    private final RegularFileProperty jar = newInputFile();
    private final Property<String> jarSha1 = getProject().getObjects().property(String.class);
    private final RegularFileProperty report = newOutputFile();

    @Inject
    public AnalyzeBytecode() {}

    @Inject
    protected WorkerExecutor getWorkerExecutor() {
        throw new UnsupportedOperationException();
    }

    @Optional
    @InputFile
    public RegularFileProperty getJar() {
        return jar;
    }

    @Input
    public Property<String> getJarSha1() {
        return jarSha1;
    }

    @Input
    public Property<String> getPluginId() {
        return pluginId;
    }

    @OutputFile
    public RegularFileProperty getReport() {
        return report;
    }

    @TaskAction
    private void doAnalysis() {
        if (!jar.isPresent()) {
            Gson gson = new Gson();
            try (OutputStream outStream = new FileOutputStream(report.getAsFile().get())) {
                IOUtils.write(gson.toJson(AnalyzeReport.noJarResolved(pluginId.get())), outStream, Charset.defaultCharset());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            getWorkerExecutor().submit(AnalysisRunnable.class, (workerConfiguration) -> {
                workerConfiguration.setIsolationMode(IsolationMode.NONE);
                workerConfiguration.params(pluginId.get(), jar.getAsFile().get(), jarSha1.get(), report.getAsFile().get());
            });
        }
    }

    public static class AnalysisRunnable implements Runnable {
        private final String pluginId;
        private final File jar;
        private final String jarSha1;
        private final File report;

        @Inject
        AnalysisRunnable(String pluginId, File jar, String jarSha1, File report) {
            this.pluginId = pluginId;
            this.jar = jar;
            this.jarSha1 = jarSha1;
            this.report = report;
        }

        @Override
        public void run() {
            final AnalyzeReport reportAnalysis = new AnalyzeReport(pluginId, jarSha1);
            ClassVisitor cl=new ClassVisitor(Opcodes.ASM6) {

                /**
                 * Called when a class is visited. This is the method called first
                 */
                @Override
                public void visit(int version, int access, String name,
                                  String signature, String superName, String[] interfaces) {
//                    System.out.println("Visiting class: "+name);
//                    System.out.println("Class Major Version: "+version);
//                    System.out.println("Super class: "+superName);
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                /**
                 * Invoked only when the class being visited is an inner class
                 */
                @Override
                public void visitOuterClass(String owner, String name, String desc) {
//                    System.out.println("Outer class: "+owner);
                    super.visitOuterClass(owner, name, desc);
                }

                /**
                 *Invoked when a class level annotation is encountered
                 */
                @Override
                public AnnotationVisitor visitAnnotation(String desc,
                                                         boolean visible) {
//                    System.out.println("Annotation: "+desc);
                    return super.visitAnnotation(desc, visible);
                }

                /**
                 * When a class attribute is encountered
                 */
                @Override
                public void visitAttribute(Attribute attr) {
//                    System.out.println("Class Attribute: "+attr.type);
                    super.visitAttribute(attr);
                }

                /**
                 *When an inner class is encountered
                 */
                @Override
                public void visitInnerClass(String name, String outerName,
                                            String innerName, int access) {
//                    System.out.println("Inner Class: "+ innerName+" defined in "+outerName);
                    super.visitInnerClass(name, outerName, innerName, access);
                }

                /**
                 * When a field is encountered
                 */
                @Override
                public FieldVisitor visitField(int access, String name,
                                               String desc, String signature, Object value) {
//                    System.out.println("Field: "+name+" "+desc+" value:"+value);
                    if (isInternalApis(desc)) {
//                        System.out.println("USING INTERNAL APIS (FIELD)");
                        reportAnalysis.getViolations().add(new InternalApiUsageViolation(desc));
                    }
                    return super.visitField(access, name, desc, signature, value);
                }


                @Override
                public void visitEnd() {
//                    System.out.println("Method ends here");
                    super.visitEnd();
                }

                /**
                 * When a method is encountered
                 */
                @Override
                public MethodVisitor visitMethod(int access, String name,
                                                 String desc, String signature, String[] exceptions) {
//                    System.out.println("Method: "+name+" "+desc);
                    return new MethodVisitor(Opcodes.ASM6) {
                        @Override
                        public void visitTypeInsn(int opcode, String type) {
//                            System.out.println("Type insn: " + type);
                            if (isInternalApis(type)) {
//                                System.out.println("USING INTERNAL APIS (INSTANTIATE)");
                                reportAnalysis.getViolations().add(new InternalApiUsageViolation(type));
                            }
                            super.visitTypeInsn(opcode, type);
                        }

                        @Override
                        public void visitParameter(String name, int access) {
//                            System.out.println("Parameter: " + name);
                            super.visitParameter(name, access);
                        }
                    };
                }

                /**
                 * When the optional source is encountered
                 */
                @Override
                public void visitSource(String source, String debug) {
//                    System.out.println("Source: "+source);
                    super.visitSource(source, debug);
                }


            };


            try {
                ArchiveInputStream archiveStream = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(new FileInputStream(jar)));
                for (ArchiveEntry entry = archiveStream.getNextEntry(); entry != null; entry = archiveStream.getNextEntry()) {

                    if (entry.isDirectory()) continue;
//                    System.out.println(entry.getName());

                    if (entry.getName().endsWith(".class")) {
//                        System.out.println(entry.getSize());
//                        System.out.println(archiveStream.available());
                        ClassReader classReader = new ClassReader(archiveStream);
                        classReader.accept(cl, 0);
                    }
                }

                Gson gson = new Gson();
                try (OutputStream outStream = new FileOutputStream(report)) {
                    IOUtils.write(gson.toJson(reportAnalysis), outStream, Charset.defaultCharset());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } catch (Exception ex) {
                Gson gson = new Gson();
                try (OutputStream outStream = new FileOutputStream(report)) {
                    IOUtils.write(gson.toJson(reportAnalysis.toAnalysisError(ex)), outStream, Charset.defaultCharset());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    private static boolean isInternalApis(String desc) {
        // TODO: Starts with 'org/gradle'
        // TODO: Ignore dep.impl. shadow package
        return desc.contains("org/gradle") && desc.contains("/internal/");
    }
}
