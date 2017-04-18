package com.ericsson.ema.tim.dml;

import java.util.Map;

public class TableInfoContext {
    private Map<String, String> tableMetadata;
    private Object tabledata;

    public Map<String, String> getTableMetadata() {
        return tableMetadata;
    }

    void setTableMetadata(Map<String, String> tableMetadata) {
        this.tableMetadata = tableMetadata;
    }

    Object getTabledata() {
        return tabledata;
    }

    void setTabledata(Object tabledata) {
        this.tabledata = tabledata;
    }

}
