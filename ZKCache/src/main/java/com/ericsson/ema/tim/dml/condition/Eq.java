package com.ericsson.ema.tim.dml.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.ericsson.ema.tim.reflection.MethodInvocationCache.AccessType.GET;
import static com.ericsson.ema.tim.reflection.MethodInvocationCache.methodInvocationCache;

public class Eq extends Clause {
    private final static Logger LOGGER = LoggerFactory.getLogger(Eq.class);

    private final String field;
    private final String valueToComp;

    private Eq(String field, String value) {
        super();
        this.field = field;
        this.valueToComp = value;
    }

    public static Eq eq(String field, String value) {
        return new Eq(field, value);
    }

    public Object getLeftOper() {
        return field;
    }

    public String getRightOper() {
        return valueToComp;
    }


    @Override
    public boolean eval(Object tuple) {
        if (this.valueToComp == null)
            return false;

        Object fieldVal;
        Method getter = methodInvocationCache.get(tuple.getClass(), field, GET);
        try {
            fieldVal = getter.invoke(tuple);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage());//should never happen
        }

        Map<String, String> metadata = getParent().getContext().getTableMetadata();

        Object finalValue;
        String fieldType = metadata.get(field);
        switch (fieldType) {
            case "string":
                finalValue = this.valueToComp;
                break;
            case "int":
                finalValue = Integer.valueOf(this.valueToComp);
                break;
            default:
                LOGGER.error("unsupported data type: {}", metadata.get(field));
                throw new RuntimeException("unsupported data type: " + metadata.get(field));
        }
        return finalValue.equals(fieldVal);
    }
}
