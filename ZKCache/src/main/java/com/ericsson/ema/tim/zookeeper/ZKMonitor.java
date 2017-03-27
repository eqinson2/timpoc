package com.ericsson.ema.tim.zookeeper;

import com.ericsson.ema.tim.AppMain;
import com.ericsson.ema.tim.javabean.JavaBeanCompiler;
import com.ericsson.ema.tim.json.JsonLoader;
import com.ericsson.ema.tim.reflection.TabDataLoader;
import com.ericsson.ema.tim.schema.Xsd2JavaBean;
import com.ericsson.ema.tim.schema.XsdRender;
import com.ericsson.ema.tim.schema.model.Table;
import com.ericsson.ema.tim.utils.FileUtils;
import com.ericsson.util.SystemPropertyUtil;
import com.ericsson.zookeeper.NodeChildCache;
import com.ericsson.zookeeper.NodeChildrenChangedListener;
import com.ericsson.zookeeper.ZooKeeperUtil;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.ericsson.ema.tim.dml.TableInfoMap.tableInfoMap;
import static com.ericsson.ema.tim.javabean.JavaBeanClassLoader.javaBeanClassLoader;
import static com.ericsson.ema.tim.reflection.Tab2ClzMap.tab2ClzMap;
import static com.ericsson.ema.tim.reflection.Tab2MethodInvocationCacheMap.tab2MethodInvocationCacheMap;
import static com.ericsson.ema.tim.zookeeper.MetaDataRegistry.metaDataRegistry;
import static org.apache.zookeeper.CreateMode.PERSISTENT;
import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;

public class ZKMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZKMonitor.class);

    private final static String APP_PACKAGE = AppMain.class.getPackage().getName();
    private final static String APP_PACKAGE_DIR = FileUtils.package2Path(APP_PACKAGE);
    private final static String FS = "/";
    private static String zkRootPath;
    private static String XSD_DIR;
    private static String JAVABEAN_DIR;
    private static String JAVABEAN_PKG;

    static {
        try {
            XSD_DIR =
                    SystemPropertyUtil.getAndAssertProperty("com.ericsson.dve.timpoc.xsddir");
        } catch (Exception e) {
            XSD_DIR = "/var/tmp/tim/xsd";
        }
        try {
            JAVABEAN_DIR =
                    SystemPropertyUtil.getAndAssertProperty("com.ericsson.dve.timpoc.javabeandir");
        } catch (Exception e) {
            JAVABEAN_DIR = "/var/tmp/tim/javabean";
        }

        try {
            JAVABEAN_PKG =
                    SystemPropertyUtil.getAndAssertProperty("com.ericsson.dve.timpoc.javabeanpkg");
        } catch (Exception e) {
            JAVABEAN_PKG = "genjavabean";
        }

        try {
            zkRootPath = SystemPropertyUtil.getAndAssertProperty("com.ericsson.dve.timpoc.zkpath");
        } catch (Exception e) {
            zkRootPath = "/TIM_POC";
        }

        try {
            FileUtils.createDir("/var/tmp/tim");
            FileUtils.createDir(XSD_DIR);
            FileUtils.createDir(JAVABEAN_DIR);
        } catch (IOException e) {
            LOGGER.error("createDir failed {}", e);
            throw new RuntimeException(e);
        }

    }

    private final ZKConnectionManager zkConnectionManager;
    private NodeChildCache nodeChildCache;

    public ZKMonitor(ZKConnectionManager zkConnectionManager) {
        this.zkConnectionManager = zkConnectionManager;
        zkConnectionManager.registerListener(new ZooKeeperConnectionStateListenerImpl());
    }

    public void start() {
        try {
            ZooKeeperUtil.createRecursive(getConnection(), zkRootPath, null, OPEN_ACL_UNSAFE, PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("Failed to start ZKMonitor, the exception is ", e);
        }
        loadAllTable();
    }

    public void stop() {
        Optional.ofNullable(nodeChildCache).ifPresent(NodeChildCache::stop);
        unloadAllTable();
    }

    private void loadAllTable() {
        unloadAllTable();

        List<String> children = new ArrayList<>();
        try {
            nodeChildCache = new NodeChildCache(getConnection(), zkRootPath, new
                    NodeChildrenChangedListenerImpl());
            children = nodeChildCache.start();
        } catch (KeeperException.ConnectionLossException e) {
            LOGGER.warn("Failed to setup nodeChildCache due to missing zookeeper connection.", e);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.warn("Failed to loadAllTable on path: [" + zkRootPath + "]", e);
        }

        childrenAdded(children);
    }

    //thread safe
    private synchronized void loadOneTable(String zkNodeName) {
        LOGGER.debug("Start to load data for node {}", zkNodeName);
        byte[] rawData = zkConnectionManager.getConnection()
                .map(zkConnection -> getDataZKNoException(zkConnection, zkRootPath + "/" + zkNodeName, new
                        NodeWatcher(zkNodeName))).orElse(new byte[0]);

        if (rawData.length == 0) {
            LOGGER.error("Failed to loadOneTable for node {}", zkNodeName);
            return;
        }
        //1. load json
        JsonLoader jloader = loadJsonFromRawData(new String(rawData), zkNodeName);
        String tableName = jloader.getTableName();

        if (!isMetaDataDefined(jloader)) {
            //metadata change-> function need re-reflection
            tableInfoMap.unregister(tableName);
            tab2MethodInvocationCacheMap.unRegister(tableName);
            tab2ClzMap.unRegister(tableName);

            //2. parse json cache and build as datamodel
            Table table = buildDataModelFromJson(jloader);
            //3. render as xsd
            String targetSchemaFile = XSD_DIR + FS + table.getName() + ".xsd";
            renderXsd(table, targetSchemaFile);
            //4. generate javabean from xsd
            generateJavaBean(targetSchemaFile);
            //5. compile javabean
            compileJavaBean();
            //6. load javabean class
            loadJavaBean(tableName);
            updateMetaData(jloader);
        }
        //7. load data by reflection, and the new data will replace old one.
        Object obj = loadDataByReflection(jloader);
        //8. register tab into global registry
        LOGGER.info("=====================register {}=====================", tableName);

        //force original loaded obj and its classloader to gc
        tableInfoMap.register(tableName, jloader.getTableMetadata(), obj);
        //System.gc();//enable -XX:+TraceClassUnloading
    }

    private boolean isMetaDataDefined(JsonLoader jsonLoader) {
        boolean defined = metaDataRegistry.isRegistered(jsonLoader.getTableName(), jsonLoader
                .getTableMetadata());
        if (defined)
            LOGGER.info("Metadata already defined for {}, skip regenerating javabean...", jsonLoader
                    .getTableName());
        else
            LOGGER.info("Metadata NOT defined for {}", jsonLoader.getTableName());
        return defined;
    }

    private void updateMetaData(JsonLoader jsonLoader) {
        metaDataRegistry.registerMetaData(jsonLoader.getTableName(), jsonLoader.getTableMetadata());
    }

    private JsonLoader loadJsonFromRawData(String json, String tableName) {
        JsonLoader jloader = new JsonLoader(tableName);
        jloader.loadJsonFromString(json);
        return jloader;
    }

    private Table buildDataModelFromJson(JsonLoader jloader) {
        LOGGER.info("=====================parse json=====================");
        Table table = XsdRender.buildModelFromMetadata(jloader.getTableName(), jloader.getTableMetadata());
        LOGGER.debug("Table structure: {}", table);
        return table;
    }

    private void renderXsd(Table table, String targetSchemaFile) {
        LOGGER.info("=====================write xsd into {}=====================", targetSchemaFile);
        try {
            FileUtils.writeFile(XsdRender.renderXsd(table), targetSchemaFile);
        } catch (IOException e) {
            LOGGER.error("writeFile failed: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private void generateJavaBean(String targetSchemaFile) {
        LOGGER.info("=====================generateJavaBean=====================");
        try {
            Xsd2JavaBean.generateJavaBean(targetSchemaFile, JAVABEAN_DIR, FileUtils.path2Package
                    (APP_PACKAGE_DIR + FS + JAVABEAN_PKG));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private void compileJavaBean() {
        LOGGER.info("=====================compile javabean=====================");
        JavaBeanCompiler compiler = new JavaBeanCompiler();
        Path path = Paths.get(JAVABEAN_DIR, APP_PACKAGE_DIR, JAVABEAN_PKG);
        compiler.setJavaSrcFileDir(Arrays.asList(path));
        try {
            compiler.compile(JavaBeanCompiler.findSrcJava(path));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void loadJavaBean(String clzName) {
        LOGGER.info("load classpath: {}", JAVABEAN_DIR);
        javaBeanClassLoader.loadClassFromClassPath(JAVABEAN_DIR, APP_PACKAGE + "." + JAVABEAN_PKG + "." +
                clzName);
    }

    private Object loadDataByReflection(JsonLoader jloader) {
        LOGGER.info("=====================load data by reflection=====================");
        String classToLoad = APP_PACKAGE + "." + JAVABEAN_PKG + "." + jloader.getTableName();
        TabDataLoader tabL = new TabDataLoader(classToLoad, jloader);
        Object obj;
        try {
            obj = tabL.loadData();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException |
                InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return obj;
    }

    private void unloadAllTable() {
        LOGGER.info("=====================unregister all table=====================");
        metaDataRegistry.clear();
        tableInfoMap.clear();
        tab2MethodInvocationCacheMap.clear();
        tab2ClzMap.clear();
    }

    private void unloadOneTable(String zkNodeName) {
        LOGGER.info("=====================register {}=====================", zkNodeName);
        metaDataRegistry.unregisterMetaData(zkNodeName);
        tableInfoMap.unregister(zkNodeName);
        tab2MethodInvocationCacheMap.unRegister(zkNodeName);
        tab2ClzMap.unRegister(zkNodeName);
    }

    private void childrenAdded(List<String> children) {
        children.forEach(this::loadOneTable);
    }

    private void childrenRemoved(List<String> children) {
        children.forEach(this::unloadOneTable);
    }

    private ZooKeeper getConnection() throws KeeperException.ConnectionLossException {
        return zkConnectionManager.getConnection().orElseThrow(KeeperException
                .ConnectionLossException::new);
    }

    private byte[] getDataZKNoException(ZooKeeper zooKeeper, String zkTarget, Watcher watcher) {
        try {
            return zooKeeper.getData(zkTarget, watcher, null);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.warn("Failed to get data from " + zkTarget, e);
            return new byte[0];
        }
    }

    private class NodeChildrenChangedListenerImpl implements NodeChildrenChangedListener {
        @Override
        public void childAdded(List<String> children) {
            childrenAdded(children);
        }

        @Override
        public void childRemoved(List<String> children) {
            childrenRemoved(children);
        }

        /**
         * Notifies that we failed to read the data from the path and register a new watcher. <br>
         * The only way out of this is to restart the {@link NodeChildCache} instance.
         *
         * @since 2.8
         */
        @Override
        public void terminallyFailed() {
            LOGGER.error("Unexpected failure happens!");
        }
    }

    private class ZooKeeperConnectionStateListenerImpl implements ZKConnectionChangeWatcher {
        @Override
        public void stateChange(State state) {
            if (state.equals(State.CONNECTED) || state.equals(State.RECONNECTED)) {
                loadAllTable();
            } else if (state.equals(State.DISCONNECTED)) {
                Optional.ofNullable(nodeChildCache).ifPresent(NodeChildCache::stop);
            }
        }
    }

    private class NodeWatcher implements Watcher {
        private final String zkNodeName;

        NodeWatcher(String zkNodeName) {
            this.zkNodeName = zkNodeName;
        }

        @Override
        public void process(WatchedEvent event) {
            if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                loadOneTable(zkNodeName);
            }
        }
    }
}
