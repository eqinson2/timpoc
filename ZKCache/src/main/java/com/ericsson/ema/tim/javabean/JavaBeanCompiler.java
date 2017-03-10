package com.ericsson.ema.tim.javabean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JavaBeanCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaBeanCompiler.class);
    private final static String CLASSPATH_DELIMITER = ";";
    private List<Path> javaSrcFileDir;

    public static List<File> findSrcJava(Path targetDirPath) throws IOException {

        LOGGER.debug("Going to find java files under {}", targetDirPath.getFileName());
        return Files.list(targetDirPath).filter(f -> f.toString().endsWith(".java")).
                map(Path::toFile).collect(Collectors.toList());
    }

    public void compile(List<File> javaFiles) throws Exception {
        if (javaFiles.isEmpty()) {
            LOGGER.warn("Empty file list for compile");
            return;
        }

        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Failed to get java compiler from PATH, Please use JDK to " +
                    "run");
        }

        LOGGER.debug("Going to compile java files: {}", Arrays.toString(javaFiles.toArray()));
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager
                .getJavaFileObjectsFromFiles(javaFiles);

        List<String> optionList = new ArrayList<>();
        optionList.addAll(Arrays.asList("-classpath", getCompileClassPath()));

        boolean isSuccess = compiler.getTask(null, fileManager, null, optionList, null,
                compilationUnits).call();
        fileManager.close();

        if (!isSuccess) {
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                LOGGER.error("Compilation failed, Error on line {} in {}", diagnostic
                        .getLineNumber(), diagnostic);
            }
        }
    }

    public void setJavaSrcFileDir(List<Path> javaSrcFileDir) {
        this.javaSrcFileDir = javaSrcFileDir;
    }

    private String getCompileClassPath() {
        String javaClassPathes = System.getProperty("java.class.path");
        if (javaSrcFileDir.isEmpty())
            return javaClassPathes;

        String newClassPath = javaSrcFileDir.stream().map(Path::toString).collect(Collectors.joining
                (CLASSPATH_DELIMITER)) +
                CLASSPATH_DELIMITER + javaClassPathes;
        LOGGER.debug("getClassPath result: {}", newClassPath);
        return newClassPath;
    }
}
