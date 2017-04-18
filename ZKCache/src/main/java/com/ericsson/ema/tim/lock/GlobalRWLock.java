package com.ericsson.ema.tim.lock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by eqinson on 2017/4/18.
 */
public enum GlobalRWLock {
    globalRWLock;

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    public void readLock() {
        rwl.readLock().lock();
    }

    public void readUnlock() {
        rwl.readLock().unlock();
    }

    public void writeLock() {
        rwl.writeLock().lock();
    }

    public void writeUnlock() {
        rwl.writeLock().unlock();
    }
}
