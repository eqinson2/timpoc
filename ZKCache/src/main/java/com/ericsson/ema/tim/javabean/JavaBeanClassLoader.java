package com.ericsson.ema.tim.javabean;

import org.slf4j.Logger;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

public enum JavaBeanClassLoader {
    javaBeanClassLoader;

    private final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JavaBeanClassLoader.class);
    private ClassLoader origClassLoader;

    public void loadClassFromClassPath(String pathStr, String clzName) {
        Path path = Paths.get(pathStr);
        try {
            URL[] urls = {path.toUri().toURL()};
            ClassLoader origClassLoader = this.getClass().getClassLoader();
            URLClassLoader urlClassLoader = new URLClassLoader(urls, origClassLoader);
            LOGGER.debug("new URLClassLoader:{}", urlClassLoader);
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            LOGGER.info("load class {} {}...", clzName, clzName + "Data");
            urlClassLoader.loadClass(clzName);
        } catch (Exception e) {
            LOGGER.error("Failed to load class", e);
            throw new RuntimeException(e);
        }
    }
}
