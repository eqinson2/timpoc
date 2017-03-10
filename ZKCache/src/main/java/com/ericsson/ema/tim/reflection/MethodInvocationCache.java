package com.ericsson.ema.tim.reflection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public enum MethodInvocationCache {
    methodInvocationCache;

    private final Logger LOGGER = LoggerFactory.getLogger(MethodInvocationCache
            .class);

    private final Map<MethodInvocationKey, Method> getterStore = new ConcurrentHashMap<>();
    private final Map<MethodInvocationKey, Method> setterStore = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    private static Method lookup(Class<?> clz, String property) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(clz);
        return Arrays.stream(beanInfo.getPropertyDescriptors()).filter(prop -> property
                .equals(prop.getName
                        ()))
                .map(PropertyDescriptor::getReadMethod).findFirst().orElseThrow(() -> new
                        RuntimeException("no such method:" + property));
    }

    public Method get(Class<?> clz, String field, AccessType accessType) {
        MethodInvocationKey key = new MethodInvocationKey(clz, field);
        Map<MethodInvocationKey, Method> store = accessType == AccessType.GET ?
                getterStore : setterStore;
        Method cached = store.get(key);
        if (cached == null) {
            lock.lock();
            try {
                cached = store.get(key);
                if (cached == null) {
                    try {
                        cached = lookup(clz, field);
                    } catch (IntrospectionException e) {
                        LOGGER.error(e.getMessage());
                    }
                    store.put(key, cached);
                }
            } finally {
                lock.unlock();
            }
        }
        return cached;
    }

    public void cleanup() {
        getterStore.clear();
        setterStore.clear();
    }

    public void cleanKey(String key) {
        getterStore.remove(key);
        setterStore.remove(key);
    }

    public enum AccessType {
        GET, SET
    }
}

class MethodInvocationKey {
    private final Class<?> lookupClass;
    private final String methodName;
    private final int hashCode;

    MethodInvocationKey(Class<?> lookupClass, String methodName) {
        this.lookupClass = lookupClass;
        this.methodName = methodName;
        int result = lookupClass != null ? lookupClass.hashCode() : 0;
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
        this.hashCode = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MethodInvocationKey that = (MethodInvocationKey) o;
        return lookupClass == that.lookupClass && methodName.equals(that.methodName);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}