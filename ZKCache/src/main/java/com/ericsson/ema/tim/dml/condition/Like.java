package com.ericsson.ema.tim.dml.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Like extends Clause {
    private final static Logger LOGGER = LoggerFactory.getLogger(Eq.class);

    private Like(String field, String value) {
        super(field, value);
    }

    public static Like like(String field, String value) {
        return new Like(field, value);
    }

    @Override
    public boolean eval(Object tuple) {
        if (this.valueToComp == null || this.valueToComp.equals(""))
            return false;

        Object fieldVal = getFiledValFromTupleByName(tuple);

        Map<String, String> metadata = getParent().getContext().getTableMetadata();
        Object valueToComp;
        String fieldType = metadata.get(field);
        switch (fieldType) {
            case "string":
                valueToComp = this.valueToComp;
                return ((String) fieldVal).contains((String) valueToComp);
            case "int":
                valueToComp = Integer.valueOf(this.valueToComp);
                return valueToComp.equals(fieldVal);
            default:
                LOGGER.error("unsupported data type: {}", metadata.get(field));
                throw new RuntimeException("unsupported data type: " + metadata.get(field));
        }
    }

}