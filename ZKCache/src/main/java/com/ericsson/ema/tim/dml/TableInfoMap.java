package com.ericsson.ema.tim.dml;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum TableInfoMap {
    tableInfoMap;

    private Map<String, TableInfoContext> map = new ConcurrentHashMap<>();

    public void registerIntoRegistry(String tablename, Map<String, String> tableMetadata, Object tabledata) {
        map.remove(tablename);//force old one to gc
        TableInfoContext context = new TableInfoContext();
        context.setTableMetadata(tableMetadata);
        context.setTabledata(tabledata);
        map.put(tablename, context);
    }

    public void clear() {
        map.clear();
    }

    public void unregisterFromRegistry(String tableName) {
        map.remove(tableName);
    }

    public TableInfoContext lookup(String tablename) {
        return map.get(tablename);
    }
}

