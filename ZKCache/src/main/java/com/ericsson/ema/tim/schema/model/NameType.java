package com.ericsson.ema.tim.schema.model;

/**
 * Created by eqinson on 2017/3/7.
 */
public class NameType {
    private String name;
    private String type;

    public NameType(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "NameType{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

