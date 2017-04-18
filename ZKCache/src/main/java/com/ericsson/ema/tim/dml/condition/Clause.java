package com.ericsson.ema.tim.dml.condition;

import com.ericsson.ema.tim.dml.Select;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.ericsson.ema.tim.reflection.MethodInvocationCache.AccessType.GET;

public abstract class Clause {
    protected final String field;
    final String valueToComp;

    private Select parent;

    Clause(String field, String value) {
        this.field = field;
        this.valueToComp = value;
    }

    Select getParent() {
        return parent;
    }

    public void setParent(Select parent) {
        this.parent = parent;
    }

    Object getFiledValFromTupleByName(Object tuple) {
        Object fieldVal;
        Method getter = getParent().getMethodInvocationCache().get(tuple.getClass(), field, GET);
        try {
            fieldVal = getter.invoke(tuple);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage());//should never happen
        }
        return fieldVal;
    }

    abstract public boolean eval(Object tuple);
}
