package com.ericsson.ema.tim.dml.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Eq extends Clause {
    private final static Logger LOGGER = LoggerFactory.getLogger(Eq.class);

    private Eq(String field, String value) {
        super(field, value);
    }

    public static Eq eq(String field, String value) {
        return new Eq(field, value);
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
                break;
            case "int":
                valueToComp = Integer.valueOf(this.valueToComp);
                break;
            default:
                LOGGER.error("unsupported data type: {}", metadata.get(field));
                throw new RuntimeException("unsupported data type: " + metadata.get(field));
        }
        return valueToComp.equals(fieldVal);
    }
}
