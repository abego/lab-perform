package org.abego.lab.perform.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class BigSampleBuilder {
    public static void main(String[] args) throws IOException {
        createSample(
                "src/test/java",
                "org.abego.lab.perform.bigsample",
                5000, 100);
    }

    private static void createSample(
            String sourceDirectory,
            String packagePath,
            int classCount, int methodsPerClassCount) throws IOException {
        File classDir = new File(sourceDirectory + "/" + packagePath.replaceAll("\\.", "/"));
        if (!classDir.isDirectory() && !classDir.mkdirs()) {
            throw new IOException("Error when creating directory: " + classDir.getAbsolutePath());
        }

        createTestClasses(packagePath, classCount, methodsPerClassCount, classDir);
        createMainClass(packagePath, classCount, methodsPerClassCount, classDir);
    }

    private static void createTestClasses(
            String packagePath, int classCount, int methodsPerClassCount, File classDir) throws IOException {
        for (int classIndex = 0; classIndex < classCount; classIndex++) {
            String className = "C" + classIndex;
            File classFile = new File(classDir, className + ".java");
            try (FileWriter fileWriter = new FileWriter(classFile);
                 PrintWriter w = new PrintWriter(fileWriter)) {

                w.printf("// automatically generated with BigSampleBuilder\n\n"+
                        "package %s;\n\n" +
                        "import org.abego.lab.perform.core.Performer;\n\n" +
                        "public class %s {\n\n", packagePath, className);
                w.printf("    public void performAllMethods() {\n");
                for (int mi = 0; mi < methodsPerClassCount; mi++) {
                    w.printf("        Performer.perform(this, \"m%d\");\n", mi);
                }
                w.printf("    }\n\n");
                for (int mi = 0; mi < methodsPerClassCount; mi++) {
                    w.printf("    public String m%1$d() {return \"%2$s#m%1$d()\";}\n",
                            mi, className);
                }
                w.printf("}\n");
            }
        }
    }

    private static void createMainClass(
            String packagePath, int classCount, int methodsPerClassCount, File classDir) throws IOException {
        String mainClassName = "BigSample";
        File classFile = new File(classDir, mainClassName+".java");
        try (FileWriter fileWriter = new FileWriter(classFile);
             PrintWriter w = new PrintWriter(fileWriter)) {

            w.printf("// automatically generated with BigSampleBuilder\n\n");
            w.printf("package %s;\n\npublic class %s {\n\n", packagePath, mainClassName);
            w.printf("    public static final int CLASS_COUNT = %d;\n", classCount);
            w.printf("    public static final int METHODS_PER_CLASS_COUNT = %d;\n", methodsPerClassCount);
            w.printf("    public static void main(String[] args) {\n");
            for (int classIndex = 0; classIndex < classCount; classIndex++) {
                String className = "C" + classIndex;
                w.printf("        new %s().performAllMethods();\n", className);
            }
            w.printf("    }\n");
            w.printf("}\n");
        }
    }
}
