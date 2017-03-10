package com.ericsson.ema.tim.json.model;

/**
 * Created by eqinson on 2017/3/7.
 */
public class FieldInfo {
    private final String fieldValue;
    private final String fieldName;
    private final String fieldType;

    public FieldInfo(String fieldValue, String fieldName, String fieldType) {
        this.fieldValue = fieldValue;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    @Override
    public String toString() {
        return "FieldInfo{" +
                "fieldValue='" + fieldValue + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", fieldType='" + fieldType + '\'' +
                '}';
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }
}
