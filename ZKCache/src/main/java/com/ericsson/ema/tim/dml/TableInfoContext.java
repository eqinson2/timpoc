package com.ericsson.ema.tim.dml;

import java.util.Map;

public class TableInfoContext {
    private Map<String, String> tableMetadata;
    private Object tabledata;

    public Map<String, String> getTableMetadata() {
        return tableMetadata;
    }

    public void setTableMetadata(Map<String, String> tableMetadata) {
        this.tableMetadata = tableMetadata;
    }

    public Object getTabledata() {
        return tabledata;
    }

    public void setTabledata(Object tabledata) {
        this.tabledata = tabledata;
    }

}
