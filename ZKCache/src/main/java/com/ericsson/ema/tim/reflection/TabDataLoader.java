package com.ericsson.ema.tim.reflection;

import com.ericsson.ema.tim.json.JsonLoader;
import com.ericsson.ema.tim.json.model.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static com.ericsson.ema.tim.reflection.Tab2ClzMap.tab2ClzMap;
import static com.ericsson.ema.tim.reflection.Tab2MethodInvocationCacheMap.tab2MethodInvocationCacheMap;

public class TabDataLoader {
    private final static Logger LOGGER = LoggerFactory.getLogger(TabDataLoader.class);
    private final static String TUPLE_FIELD = "records";
    private final MethodInvocationCache cache;

    private final String classToLoad;
    private final JsonLoader jloader;

    public TabDataLoader(String clzToLoad, JsonLoader jloader) {
        this.classToLoad = clzToLoad;
        this.jloader = jloader;
        cache = tab2MethodInvocationCacheMap.lookup(jloader.getTableName());
    }

    private static Object realFieldVal(FieldInfo field) {
        Object value = null;
        switch (field.getFieldType()) {
            case "string":
                value = field.getFieldValue();
                break;
            case "int":
                value = Integer.valueOf(field.getFieldValue());
                break;
            default:
                LOGGER.error("unsupported data type: {}", field.getFieldType());
        }
        return value;
    }

    public Object loadData() throws ClassNotFoundException, IllegalAccessException,
            InstantiationException,
            InvocationTargetException {
        LOGGER.info("=====================reflect class: {}=====================", classToLoad);
        Class<?> clz = tab2ClzMap.lookup(jloader.getTableName());
        if (clz == null) {
            clz = Thread.currentThread().getContextClassLoader().loadClass(classToLoad);
            tab2ClzMap.register(jloader.getTableName(), clz);
        }

        Object obj = clz.newInstance();

        JavaBeanReflectionProxy proxy = new JavaBeanReflectionProxy(obj);
        LOGGER.debug("init getTupleListType: {}", proxy.getTupleListType());

        Method getter = cache.get(clz, TUPLE_FIELD, MethodInvocationCache.AccessType.GET);
        List<Object> records = (List<Object>) getter.invoke(obj);//it will create a list internally

        List<List<FieldInfo>> rowcol = jloader.getTupleList();
        for (List<FieldInfo> row : rowcol) {
            Object tuple = null;
            try {
                tuple = proxy.getTupleListType().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            records.add(tuple);
            for (FieldInfo field : row) {
                try {
                    fillinField(tuple, field, realFieldVal(field));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return obj;
    }

    private void fillinField(Object tuple, FieldInfo field, Object value) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(tuple.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

        Arrays.stream(propertyDescriptors)
                .filter(prop -> field.getFieldName().equals(prop.getName()))
                .findFirst()
                .ifPresent(
                        prop -> {
                            Method setter = prop.getWriteMethod();
                            try {
                                setter.invoke(tuple, value);
                                LOGGER.debug("fillinField : {} = {}", field.getFieldName(), value);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                                LOGGER.error("error fillinField : {}", field);
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                                LOGGER.error("IllegalArgumentException fillinField : {}", field);
                            }
                        });
    }
}
