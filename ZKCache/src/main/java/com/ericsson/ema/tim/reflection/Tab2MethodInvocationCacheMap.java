package com.ericsson.ema.tim.reflection;

import java.util.HashMap;
import java.util.Map;

public enum Tab2MethodInvocationCacheMap {
    tab2MethodInvocationCacheMap;

    private Map<String, MethodInvocationCache> map = new HashMap<>();

    public synchronized void register(String tablename, MethodInvocationCache cache) {
        map.put(tablename, cache);
    }

    public synchronized void clear() {
        map.clear();
    }

    public synchronized void unRegister(String tableName) {
        MethodInvocationCache cache = map.remove(tableName);
        if (cache != null)
            cache.cleanup();
    }

    public synchronized MethodInvocationCache lookup(String tablename) {
        if (!map.containsKey(tablename)) {
            MethodInvocationCache cache = new MethodInvocationCache();
            map.put(tablename, cache);
            return cache;
        }
        return map.get(tablename);
    }
}
