package com.ericsson.ema.tim.javabean;

import org.slf4j.Logger;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

public enum JavaBeanClassLoader {
    javaBeanClassLoader;

    private final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JavaBeanClassLoader.class);


    public void loadClassFromClassPath(String pathStr, String clzName) {
        if (pathStr == null || "".equals(pathStr)) {
            return;
        }

        Path path = Paths.get(pathStr);
        try {
            URL[] urls = {path.toUri().toURL()};
            URLClassLoader urlClassLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(urlClassLoader);
//            Class<?> urlClass = URLClassLoader.class;
//            Method method = urlClass.getDeclaredMethod("addURL", URL.class);
//            method.setAccessible(true);
//            LOGGER.info("add {} to system class path...", pathStr);
//            method.invoke(urlClassLoader, path.toUri().toURL());

            LOGGER.info("load class {} {}...", clzName, clzName + "Data");
            urlClassLoader.loadClass(clzName);
            urlClassLoader.loadClass(clzName + "Data");
        } catch (Exception e) {
            LOGGER.error("Failed to load class", e);
            throw new RuntimeException(e);
        }
    }
}
