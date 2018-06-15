package org.gradleplugins.tasks;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.aQute.bnd.build.Run;
import org.objectweb.asm.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class AnalyzeBytecode extends DefaultTask {
    private final RegularFileProperty jar = newInputFile();

    public RegularFileProperty getJar() {
        return jar;
    }

    @TaskAction
    private void doAnalysis() {

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
                    throw new RuntimeException("Using internal APIS (field) " + desc);
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
                return new MethodVisitor(Opcodes.ASM4) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        System.out.println("Type insn: " + type);
                        if (isInternalApis(type)) {
                            System.out.println("USING INTERNAL APIS (INSTANTIATE)");
                            throw new RuntimeException("Using internal APIS (instantiate) " + type);
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
            ArchiveInputStream archiveStream = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(new FileInputStream(jar.get().getAsFile())));
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
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean isInternalApis(String desc) {
        return desc.contains("org/gradle") && desc.contains("/internal/");
    }
}
