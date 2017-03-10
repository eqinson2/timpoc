package com.ericsson.ema.tim.zookeeper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum MetaDataRegistry {
    metaDataRegistry;

    private Map<String, Map<String, String>> registry = new ConcurrentHashMap<>();

    public void registerMetaData(String tableName, Map<String, String> metadata) {
        registry.put(tableName, metadata);
    }

    public void unregisterMetaData(String tableName) {
        registry.remove(tableName);
    }

    public void unregisterAll() {
        registry.clear();
    }

    public boolean isRegistered(String tableName, Map<String, String> other) {
        return registry.containsKey(tableName) && registry.get(tableName).equals(other);
    }
}
