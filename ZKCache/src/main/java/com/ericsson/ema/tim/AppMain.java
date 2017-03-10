package com.ericsson.ema.tim;

import com.ericsson.ema.tim.javabean.JavaBeanClassLoader;
import com.ericsson.ema.tim.javabean.JavaBeanCompiler;
import com.ericsson.ema.tim.json.JsonLoader;
import com.ericsson.ema.tim.reflection.TabDataLoader;
import com.ericsson.ema.tim.schema.Xsd2JavaBean;
import com.ericsson.ema.tim.schema.XsdRender;
import com.ericsson.ema.tim.schema.model.Table;
import com.ericsson.ema.tim.utils.FileUtils;
import com.ericsson.ema.tim.zookeeper.ZKMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static com.ericsson.ema.tim.dml.Select.select;
import static com.ericsson.ema.tim.dml.TableInfoMap.tableInfoMap;
import static com.ericsson.ema.tim.dml.condition.Eq.eq;
import static com.ericsson.ema.tim.javabean.JavaBeanClassLoader.javaBeanClassLoader;
import static com.ericsson.ema.tim.zookeeper.ZKConnectionManager.zkConnectionManager;

public class AppMain {
    private final static String APP_PACKAGE = AppMain.class.getPackage().getName();
    private final static String APP_PACKAGE_DIR = FileUtils.package2Path(APP_PACKAGE);

    private final static Logger LOGGER = LoggerFactory.getLogger(JavaBeanClassLoader.class);
    private final static String XSD_DIR = "xsd";
    private final static String JAVABEAN_DIR = "javabean";
    private final static String JAVABEAN_PKG = "genjavabean";
    private final static String FS = "/";

    private final static String PROPERTY_FILE = "etc/system.properties";

    static {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(PROPERTY_FILE));
        } catch (IOException e) {
            LOGGER.info("no custome system properties: {}");
        }
        for (String name : p.stringPropertyNames()) {
            String value = p.getProperty(name);
            System.setProperty(name, value);
            LOGGER.debug("system property: {} = {}", name, value);
        }
    }

    public static void main(String[] args) {
        //doTest();
        zkConnectionManager.init();
        ZKMonitor zkMonitor = new ZKMonitor(zkConnectionManager);
        zkMonitor.start();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000 * 60);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private static void doTest() throws Exception {
        //1. load json
        String jfname = "json/test.json";
        LOGGER.info("=====================load json: {}=====================", jfname);
        JsonLoader jloader = new JsonLoader();
        jloader.loadJsonFromFile(jfname);

        String tableName = jloader.getTableName();

        //2. parse json cache and build as datamodel
        LOGGER.info("=====================parse json=====================");
        Table table = XsdRender.buildModelFromMetadata(tableName, jloader.getTableMetadata());
        LOGGER.debug("Table structure: {}", table);

        //3. render as xsd
        String targetSchemaFile = XSD_DIR + FS + tableName + ".xsd";
        LOGGER.info("=====================write xsd into {}=====================", targetSchemaFile);
        try {
            FileUtils.writeFile(XsdRender.renderXsd(table), targetSchemaFile);
        } catch (IOException e) {
            LOGGER.error("writeFile failed: " + e.getMessage());
            throw e;
        }

        //4. generate javabean from xsd
        LOGGER.info("=====================generateJavaBean=====================");
        Xsd2JavaBean.generateJavaBean(targetSchemaFile, JAVABEAN_DIR, FileUtils.path2Package
                (APP_PACKAGE_DIR + FS + JAVABEAN_PKG));

        //5. compile javabean
        LOGGER.info("=====================compile javabean=====================");
        JavaBeanCompiler compiler = new JavaBeanCompiler();
        String packege = JavaBeanCompiler.class.getPackage().getName();
        Path path = Paths.get(JAVABEAN_DIR, APP_PACKAGE_DIR, JAVABEAN_PKG);
        compiler.setJavaSrcFileDir(Arrays.asList(path));
        compiler.compile(JavaBeanCompiler.findSrcJava(path));

        //6. load javabean class
        LOGGER.info("load classpath: {}", JAVABEAN_DIR);
        javaBeanClassLoader.loadClassFromClassPath(JAVABEAN_DIR, APP_PACKAGE + "." + JAVABEAN_PKG + "." +
                tableName);

        //7. load data by reflection
        LOGGER.info("=====================load data by reflection=====================");
        String classToLoad = APP_PACKAGE + "." + JAVABEAN_PKG + "." + table.getName();
        TabDataLoader tabL = new TabDataLoader(classToLoad, jloader);
        Object obj = tabL.loadData();

        //8. register tab
        tableInfoMap.registerIntoRegistry(tableName, jloader.getTableMetadata(), obj);

        //9. select:
        LOGGER.info("=====================select some data for testing=====================");
        List<Object> result = select().from(tableName).where(eq("name", "eqinson1")).where(eq("age",
                "1")).execute();
        System.out.println(result.size());

        result = select().from(tableName).where(eq("name", "eqinson2")).where(eq("age", "2"))
                .execute();
        System.out.println(result.size());

        result = select().from(tableName).where(eq("name", "eqinson4")).where(eq("age", "4")).where(eq
                ("job", "manager")).execute();
        System.out.println(result.size());

        List<Object> sliceRes = select("age", "job").from(tableName).where(eq("name", "eqinson4"))
                .where(eq("age", "4")).where(eq("job", "manager")).execute();
        for (Object eachRow : sliceRes) {
            if (eachRow instanceof List<?>) {
                List<Object> row = (List<Object>) eachRow;
                row.forEach(System.out::println);
            }
        }

        LOGGER.info("=====================performance testing=====================");
        performanceTest(tableName);
    }

    private static void performanceTest(String tableName) {
        long start = System.currentTimeMillis();
        int LOOPNUM = 100000;
        for (int i = 0; i < LOOPNUM; i++) {
            select().from(tableName).where(eq("name", "eqinson1")).where(eq("age", "31")).execute();
            select().from(tableName).where(eq("name", "eqinson2")).where(eq("age", "33")).execute();
            select().from(tableName).where(eq("name", "eqinson4")).where(eq("age", "34")).where(eq
                    ("job", "manager")).execute();
            select("age", "job").from(tableName).where(eq("name", "eqinson4"))
                    .where(eq("age", "34")).where(eq("job", "manager")).execute();
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}
