package com.ericsson.ema.tim.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class ZKClient implements Watcher {
    private final static String rootPath = "/TIM_POC";
    private final static Logger LOGGER = LoggerFactory.getLogger(ZKClient.class);
    private final static String TABLE_TAG = "Table";
    private final static String ID_TAG = "Id";
    private final static String jsonFile = "json/test.json";

    private ZooKeeper zk;

    public static void main(String[] args) throws IOException {
        ZKClient zkc = new ZKClient();
        zkc.createZkClient();

        if (!zkc.exists(rootPath)) {
            LOGGER.debug("create znode {}" + rootPath);
            zkc.createPersistentNode(rootPath, "");
        }

        String jsonStr = FileUtils.readFile(jsonFile);
        String tab = zkc.parseTableName();

        String tabPath = rootPath + "/" + tab;
        if (!zkc.exists(tabPath)) {
            LOGGER.debug("create znode {}" + tabPath);
            zkc.createPersistentNode(tabPath, jsonStr);
        } else {
            LOGGER.debug("set znode {}" + tabPath);
            zkc.setNodeData(tabPath, jsonStr);
        }

        LOGGER.info("current root data: {}", zkc.getNodeData(rootPath));
        LOGGER.info("{} children {}", rootPath, zkc.getChildren(rootPath));
        LOGGER.info("{} data: {}", tabPath, zkc.getNodeData(tabPath));
        zkc.closeZk();
    }

    private String parseTableName() throws IOException {
        String jsonStr = FileUtils.readFile(this.jsonFile);
        JSONObject obj = new JSONObject(jsonStr);
        JSONObject table = obj.getJSONObject(TABLE_TAG);
        return table.getString(ID_TAG);
    }

    /**
     * 创建zookeeper客户端
     *
     * @return
     */
    private boolean createZkClient() {
        try {
            zk = new ZooKeeper(PropertiesDynLoading.connectString, PropertiesDynLoading.sessionTimeout, this);
        } catch (IOException e) {
            LOGGER.error("{}", e);
            e.printStackTrace();
            return false;
        }
        if (PropertiesDynLoading.authentication) {
            zk.addAuthInfo(PropertiesDynLoading.authScheme, PropertiesDynLoading.accessKey.getBytes());
        }
        if (!isConnected()) {
            LOGGER.debug(" ZooKeeper client state [{}]", zk.getState().toString());
        }
        try {
            if (zk.exists("/zookeeper", false) != null) {
                LOGGER.debug("create ZooKeeper Client Success! connectString", PropertiesDynLoading
                    .connectString);
                LOGGER.debug(" ZooKeeper client state [{}]", zk.getState());
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("create ZooKeeper Client Fail! connectString", PropertiesDynLoading.connectString);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 新增持久化节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return
     */
    private boolean createPersistentNode(String path, String data) {
        if (isConnected()) {

            try {
                if (PropertiesDynLoading.authentication) {
                    zk.create(path, data.getBytes(), getAdminAcls(), CreateMode.PERSISTENT);
                } else {
                    zk.create(path, data.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("{}", e);
                return false;
            }

        }
        LOGGER.debug("zookeeper state", zk.getState());
        return false;
    }

    /**
     * 创建瞬时节点
     *
     * @param path
     * @param data
     * @return
     */
    private boolean creatEphemeralNode(String path, String data) {
        if (isConnected()) {

            try {
                if (PropertiesDynLoading.authentication) {
                    zk.create(path, data.getBytes(), getAdminAcls(), CreateMode.PERSISTENT);
                } else {
                    zk.create(path, data.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("{}", e);
                return false;
            }

        }
        LOGGER.debug("zookeeper state", zk.getState());
        return false;
    }

    /**
     * 修改数据
     *
     * @param path
     * @param data
     * @return
     */
    private boolean setNodeData(String path, String data) {
        if (isConnected()) {
            try {
                zk.setData(path, data.getBytes(), -1);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("{}", e);
                return false;
            }
        }
        LOGGER.debug("zookeeper state = [{}]", zk.getState());
        return false;
    }

    /**
     * 删除节点
     *
     * @param path
     * @return
     */
    private boolean deleteNode(String path) {
        if (isConnected()) {
            try {
                zk.delete(path, -1);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("{}", e);
                return false;
            }
        }
        LOGGER.debug("zookeeper state = [{}]", zk.getState());
        return false;
    }

    /**
     * 获取节点值
     *
     * @param path
     * @return
     */
    public String getNodeData(String path) {
        if (isConnected()) {
            String data = null;
            try {
                byte[] byteData = zk.getData(path, true, null);
                data = new String(byteData, "utf-8");
                return data;
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("{}", e);
                return null;
            }
        }
        LOGGER.debug("zookeeper state = [{}]", zk.getState());
        return null;
    }

    /**
     * 获取path子节点名列表
     *
     * @param path
     * @return
     */
    public List<String> getChildren(String path) {
        if (isConnected()) {
            String data = null;
            try {
                return zk.getChildren(path, false);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("{}", e);
                return null;
            }
        }
        LOGGER.debug("zookeeper state = [{}]", zk.getState());
        return null;
    }

    public boolean startZkClient() {
        return createZkClient();
    }

    /**
     * zookeeper是否连接服务器
     *
     * @return
     */
    public boolean isConnected() {
        return zk.getState().isConnected();
    }

    /**
     * 是否存在path路径节点
     *
     * @param path
     * @return
     */
    public boolean exists(String path) {
        try {
            return zk.exists(path, false) != null;
        } catch (Exception e) {
            LOGGER.error("{}", e);
        }
        return false;
    }

    /**
     * 关闭zookeeper
     */
    public void closeZk() {
        if (isConnected()) {
            try {
                zk.close();
                LOGGER.debug("close zookeeper [{}]", "success");
            } catch (InterruptedException e) {
                LOGGER.error("zookeeper state = [{}]", e);
                e.printStackTrace();
            }
        } else {
            LOGGER.debug("zookeeper state = [{}]", zk.getState());
        }

    }

    /**
     * @return
     */
    public List<ACL> getCreateNodeAcls() {
        List<ACL> listAcls = new ArrayList<ACL>(3);
        try {
            Id id = new Id(PropertiesDynLoading.authScheme,
                DigestAuthenticationProvider.generateDigest(PropertiesDynLoading.accessKey));
            ACL acl = new ACL(Perms.CREATE, id);
            listAcls.add(acl);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return Ids.OPEN_ACL_UNSAFE;
        }
        return listAcls;
    }

    public List<ACL> getAdminAcls() {
        List<ACL> listAcls = new ArrayList<ACL>(3);
        try {
            Id id = new Id(PropertiesDynLoading.authScheme,
                DigestAuthenticationProvider.generateDigest(PropertiesDynLoading.accessKey));
            ACL acl = new ACL(Perms.ALL, id);
            listAcls.add(acl);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return Ids.OPEN_ACL_UNSAFE;
        }
        return listAcls;
    }

    @Override
    public void process(WatchedEvent event) {
    }

    private static class PropertiesDynLoading {
        //        static final String connectString = "10.175.146.36:6181";
        static final String connectString = "localhost:6181";
        static final int sessionTimeout = 30000;
        static final String authScheme = "digest";
        static final String accessKey = "cache:svcctlg";
        static final boolean authentication = false;
    }

}
