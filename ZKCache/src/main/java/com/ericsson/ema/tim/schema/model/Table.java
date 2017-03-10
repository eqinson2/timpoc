package com.ericsson.ema.tim.schema.model;

/**
 * Created by eqinson on 2017/3/7.
 */

public final class Table {
    private final String name;
    private final TableTuple records;

    public Table(String name, TableTuple tuples) {
        this.name = name;
        this.records = tuples;
    }

    @Override
    public String toString() {
        return "Table{" +
                "name='" + name + '\'' +
                ", records=" + records +
                '}';
    }

    public TableTuple getRecords() {
        return records;
    }

    public String getName() {
        return name;
    }
}
