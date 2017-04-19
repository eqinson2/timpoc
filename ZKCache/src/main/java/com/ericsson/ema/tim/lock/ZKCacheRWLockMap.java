package com.ericsson.ema.tim.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by eqinson on 2017/4/18.
 */
public enum ZKCacheRWLockMap {
    zkCacheRWLock;

    private final Map<String, ZKCacheRWLock> map = new ConcurrentHashMap<>();

    public void readLockTable(String table) {
        map.computeIfAbsent(table, k -> new ZKCacheRWLock()).readLock();
    }

    public void readUnLockTable(String table) {
        if (!map.containsKey(table))
            throw new IllegalStateException("Not gain table read lock yet before unlock");

        map.get(table).readUnlock();
    }

    public void writeLockTable(String table) {
        map.computeIfAbsent(table, k -> new ZKCacheRWLock()).writeLock();
    }

    public void writeUnLockTable(String table) {
        if (!map.containsKey(table))
            throw new IllegalStateException("Not gain read lock yet before unlock");

        map.get(table).writeUnlock();
    }


    private static class ZKCacheRWLock {
        private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

        void readLock() {
            rwl.readLock().lock();
        }

        void readUnlock() {
            rwl.readLock().unlock();
        }

        void writeLock() {
            rwl.writeLock().lock();
        }

        void writeUnlock() {
            rwl.writeLock().unlock();
        }
    }


}
