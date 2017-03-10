package com.ericsson.ema.tim.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
    }

    public static Path getPath(String resource) throws URISyntaxException {
        URL url = FileUtils.class.getClassLoader().getResource(resource);
        return url != null ? Paths.get(url.toURI()) : null;
    }

    public static String readFile(String path) throws IOException {
        final byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, Charset.defaultCharset());
    }

    public static void writeFile(String content, String fname) throws IOException {
        try (PrintStream out = new PrintStream(new FileOutputStream(fname))) {
            out.print(content);
            out.flush();
        }
    }

    public static void createDir(String dirName) throws IOException {
        Path path = Paths.get(dirName);
        if (!Files.exists(path)) {
            LOGGER.debug("create new dir: {}", dirName);
            Files.createDirectories(path);
        } else {
            LOGGER.debug("dir already exist: {}", dirName);
        }
    }

//    public static void rmDirRecursively(String path) throws IOException {
//        Path rootPath = Paths.get(path);
//        if (Files.exists(rootPath) && Files.isDirectory(rootPath))
//            Files.walk(rootPath)
//                    .sorted(Comparator.reverseOrder())
//                    .filter(p -> !p.toFile().getName().equals(path))//skip root dir
//                    .peek(f -> LOGGER.debug("delete file {}", f))
//                    .forEach(p -> {
//                        try {
//                            Files.delete(p);
//                        } catch (NoSuchFileException ex) {
//                            LOGGER.error("{}: no such" + " file or directory", p);
//                        } catch (DirectoryNotEmptyException ex) {
//                            LOGGER.error("{} not empty", p);
//                            File dir = p.toFile();
//                            String[] files = dir.list();
//                            if (files.length == 0) {
//                                LOGGER.debug("The directory is empty");
//                            } else {
//                                for (String aFile : files) {
//                                    LOGGER.debug(aFile);
//                                }
//                            }
//                        } catch (IOException ex) {
//                            LOGGER.error(ex.getMessage());
//                        }
//                    });
//    }

    public static void rmDirRecursively(String dirName) throws IOException {
        Path path = Paths.get(dirName);
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                LOGGER.debug("delete file {}", file.getFileName());
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (dir.toFile().getName().equals(dirName))
                    return FileVisitResult.CONTINUE;
                else if (exc == null) {
                    LOGGER.debug("delete dir {}", dir.getFileName());
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }

    public static String package2Path(String packageName) {
        return packageName.replace(".", "/");
    }

    public static String path2Package(String pathName) {
        return pathName.replace("/", ".");
    }
}
