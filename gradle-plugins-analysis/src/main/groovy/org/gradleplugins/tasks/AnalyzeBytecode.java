package org.gradleplugins.tasks;

import com.google.gson.Gson;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerExecutor;
import org.gradleplugins.AnalyzeReport;
import org.gradleplugins.AnalyzeViolation;
import org.objectweb.asm.*;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.Charset;

public class AnalyzeBytecode extends DefaultTask {
    private final Property<String> pluginId = getProject().getObjects().property(String.class);
    private final RegularFileProperty jar = newInputFile();
    private final RegularFileProperty report = newOutputFile();

    @Inject
    public AnalyzeBytecode() {}

    @Inject
    protected WorkerExecutor getWorkerExecutor() {
        throw new UnsupportedOperationException();
    }

    @InputFile
    public RegularFileProperty getJar() {
        return jar;
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
        getWorkerExecutor().submit(AnalysisRunnable.class, (workerConfiguration) -> {
            workerConfiguration.setIsolationMode(IsolationMode.NONE);
            workerConfiguration.params(pluginId.get(), jar.getAsFile().get(), report.getAsFile().get());
        });
    }

    public static class AnalysisRunnable implements Runnable {
        private final String pluginId;
        private final File jar;
        private final File report;

        @Inject
        AnalysisRunnable(String pluginId, File jar, File report) {
            this.pluginId = pluginId;
            this.jar = jar;
            this.report = report;
        }

        @Override
        public void run() {
            AnalyzeReport reportAnalysis = new AnalyzeReport(pluginId);
            ClassVisitor cl=new ClassVisitor(Opcodes.ASM4) {

                /**
                 * Called when a class is visited. This is the method called first
                 */
                @Override
                public void visit(int version, int access, String name,
                                  String signature, String superName, String[] interfaces) {
                    System.out.println("Visiting class: "+name);
                    System.out.println("Class Major Version: "+version);
                    System.out.println("Super class: "+superName);
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                /**
                 * Invoked only when the class being visited is an inner class
                 */
                @Override
                public void visitOuterClass(String owner, String name, String desc) {
                    System.out.println("Outer class: "+owner);
                    super.visitOuterClass(owner, name, desc);
                }

                /**
                 *Invoked when a class level annotation is encountered
                 */
                @Override
                public AnnotationVisitor visitAnnotation(String desc,
                                                         boolean visible) {
                    System.out.println("Annotation: "+desc);
                    return super.visitAnnotation(desc, visible);
                }

                /**
                 * When a class attribute is encountered
                 */
                @Override
                public void visitAttribute(Attribute attr) {
                    System.out.println("Class Attribute: "+attr.type);
                    super.visitAttribute(attr);
                }

                /**
                 *When an inner class is encountered
                 */
                @Override
                public void visitInnerClass(String name, String outerName,
                                            String innerName, int access) {
                    System.out.println("Inner Class: "+ innerName+" defined in "+outerName);
                    super.visitInnerClass(name, outerName, innerName, access);
                }

                /**
                 * When a field is encountered
                 */
                @Override
                public FieldVisitor visitField(int access, String name,
                                               String desc, String signature, Object value) {
                    System.out.println("Field: "+name+" "+desc+" value:"+value);
                    if (isInternalApis(desc)) {
                        System.out.println("USING INTERNAL APIS (FIELD)");
                        reportAnalysis.getViolations().add(new AnalyzeViolation("Using internal APIS (field) " + desc));
                    }
                    return super.visitField(access, name, desc, signature, value);
                }


                @Override
                public void visitEnd() {
                    System.out.println("Method ends here");
                    super.visitEnd();
                }

                /**
                 * When a method is encountered
                 */
                @Override
                public MethodVisitor visitMethod(int access, String name,
                                                 String desc, String signature, String[] exceptions) {
                    System.out.println("Method: "+name+" "+desc);
                    return new MethodVisitor(Opcodes.ASM5) {
                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            System.out.println("Type insn: " + type);
                            if (isInternalApis(type)) {
                                System.out.println("USING INTERNAL APIS (INSTANTIATE)");
                                reportAnalysis.getViolations().add(new AnalyzeViolation("Using internal APIS (instantiate) " + type));
                            }
                            super.visitTypeInsn(opcode, type);
                        }

                        @Override
                        public void visitParameter(String name, int access) {
                            System.out.println("Parameter: " + name);
                            super.visitParameter(name, access);
                        }
                    };
                }

                /**
                 * When the optional source is encountered
                 */
                @Override
                public void visitSource(String source, String debug) {
                    System.out.println("Source: "+source);
                    super.visitSource(source, debug);
                }


            };


            try {
                ArchiveInputStream archiveStream = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(new FileInputStream(jar)));
                for (ArchiveEntry entry = archiveStream.getNextEntry(); entry != null; entry = archiveStream.getNextEntry()) {

                    if (entry.isDirectory()) continue;
                    System.out.println(entry.getName());

                    if (entry.getName().endsWith(".class")) {
                        System.out.println(entry.getSize());
                        System.out.println(archiveStream.available());
                        ClassReader classReader = new ClassReader(archiveStream);
                        classReader.accept(cl, 0);
                    }
                }

                Gson gson = new Gson();
                try (OutputStream outStream = new FileOutputStream(report)) {
                    IOUtils.write(gson.toJson(report), outStream, Charset.defaultCharset());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static boolean isInternalApis(String desc) {
        // TODO: Starts with 'org/gradle'
        // TODO: Ignore dep.impl. shadow package
        return desc.contains("org/gradle") && desc.contains("/internal/");
    }
}
