package com.ericsson.ema.tim.reflection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Tab2ClzMap {
    tab2ClzMap;

    private Map<String, Class<?>> registry = new HashMap<>();

    public void register(String tableName, Class<?> clz) {
        registry.put(tableName, clz);
    }

    public Optional<Class<?>> lookup(String tableName) {
        return Optional.ofNullable(registry.get(tableName));
    }

    public void unRegister(String tableName) {
        registry.remove(tableName);
    }

    public void clear() {
        registry.clear();
    }

}
