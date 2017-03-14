package com.ericsson.ema.tim.zookeeper;

import java.util.HashMap;
import java.util.Map;

public enum MetaDataRegistry {
    metaDataRegistry;

    private Map<String, Map<String, String>> registry = new HashMap<>();

    public void registerMetaData(String tableName, Map<String, String> metadata) {
        registry.put(tableName, metadata);
    }

    public void unregisterMetaData(String tableName) {
        registry.remove(tableName);
    }

    public void clear() {
        registry.clear();
    }

    public boolean isRegistered(String tableName, Map<String, String> other) {
        return registry.containsKey(tableName) && registry.get(tableName).equals(other);
    }
}
