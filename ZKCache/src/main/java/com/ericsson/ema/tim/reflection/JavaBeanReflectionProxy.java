package com.ericsson.ema.tim.reflection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;

public class JavaBeanReflectionProxy {
    private final static String TUPLENAME = "records";
    private final static Logger LOGGER = LoggerFactory.getLogger(JavaBeanReflectionProxy.class);

    private Object instance;
    private Class<?> tupleListType;

    public JavaBeanReflectionProxy(Object instance) {
        this.instance = instance;
        tupleListType = getTupleListTypeInfo().orElseGet(null);
    }

    public Class<?> getTupleListType() {
        return tupleListType;
    }

    private Optional<? extends Class<?>> getTupleListTypeInfo() {
        Field field = Arrays.stream(instance.getClass().getDeclaredFields()).filter(f ->
                TUPLENAME.equals(f.getName())).findFirst().orElseThrow(() -> new RuntimeException
                ("no such field :" + TUPLENAME));

        Type genericFieldType = field.getGenericType();
        if (genericFieldType instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) genericFieldType;
            Type[] fieldArgTypes = aType.getActualTypeArguments();
            return fieldArgTypes == null ? Optional.empty() : Arrays.stream(fieldArgTypes).map(t ->
                    (Class<?>) t).findFirst();
        }

        return Optional.empty();
    }

    private Object getTupleListInstance() {
        Object value = null;
        try {
            Field field = instance.getClass().getDeclaredField(TUPLENAME);
            field.setAccessible(true);
            value = field.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error(e.getMessage());
        }
        LOGGER.info("getTupleListInstance: " + value);
        return value;
    }
}
