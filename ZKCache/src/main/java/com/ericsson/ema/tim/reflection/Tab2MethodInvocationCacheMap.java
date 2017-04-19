package com.ericsson.ema.tim.reflection;

import java.util.HashMap;
import java.util.Map;

public enum Tab2MethodInvocationCacheMap {
    tab2MethodInvocationCacheMap;

    private Map<String, MethodInvocationCache> map = new HashMap<>();

    public void clear() {
        map.clear();
    }

    public void unRegister(String tableName) {
        MethodInvocationCache cache = map.remove(tableName);
        if (cache != null)
            cache.cleanup();
    }

    public MethodInvocationCache lookup(String tablename) {
//        if (!map.containsKey(tablename)) {
//            MethodInvocationCache cache = new MethodInvocationCache();
//            map.put(tablename, cache);
//            return cache;
//        }
//        return map.get(tablename);
        return map.computeIfAbsent(tablename, k -> new MethodInvocationCache());
    }
}
