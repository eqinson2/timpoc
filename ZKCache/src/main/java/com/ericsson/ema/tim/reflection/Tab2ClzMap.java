package com.ericsson.ema.tim.reflection;

import java.util.HashMap;
import java.util.Map;

public enum Tab2ClzMap {
    tab2ClzMap;

    private Map<String, Class<?>> registry = new HashMap<>();

    public void register(String tableName, Class<?> clz) {
        registry.put(tableName, clz);
    }

    public Class<?> lookup(String tableName) {
        return registry.get(tableName);
    }

    public void unRegister(String tableName) {
        registry.remove(tableName);
    }

    public void clear() {
        registry.clear();
    }

}
