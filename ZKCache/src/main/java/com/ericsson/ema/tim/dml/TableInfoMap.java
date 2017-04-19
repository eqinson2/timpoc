package com.ericsson.ema.tim.dml;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum TableInfoMap {
    tableInfoMap;

    private Map<String, TableInfoContext> map = new HashMap<>();

    public void registerOrReplace(String tablename, Map<String, String> tableMetadata, Object tabledata) {
        map.compute(tablename, (k, v) -> {
            TableInfoContext context = new TableInfoContext();
            context.setTableMetadata(tableMetadata);
            context.setTabledata(tabledata);
            return context;
        });
    }

    public void clear() {
        map.clear();
    }

    public void unregister(String tableName) {
        map.remove(tableName);
    }

    public Optional<TableInfoContext> lookup(String tablename) {
        return Optional.ofNullable(map.get(tablename));
    }
}

