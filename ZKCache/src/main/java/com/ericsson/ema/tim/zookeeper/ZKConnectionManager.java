package com.ericsson.ema.tim.zookeeper;

import com.ericsson.util.SystemPropertyUtil;
import com.ericsson.zookeeper.ZooKeeperUtil;
import javascalautils.concurrent.NamedSequenceThreadFactory;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

/**
 * The connection manager used to maintain Z* connections
 *
 * @author eweiych
 */
public enum ZKConnectionManager {
    zkConnectionManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZKConnectionManager.class);

    private static final int SESSION_TIMEOUT = 6000;
    private String connectStr;
    private Optional<ZooKeeper> zooKeeper = Optional.empty();

    private Set<ZKConnectionChangeWatcher> listeners = new HashSet<>();

    private volatile boolean waitForReconnect = false;

    private ExecutorService reconnectExecutor;

    private Future reconnFuture;

    ZKConnectionManager() {
        try {
            connectStr = SystemPropertyUtil.getAndAssertProperty("com.ericsson.dve.timpoc.zkconnstr");
        } catch (IllegalArgumentException e) {
            connectStr = "localhost:6181";
        }
    }

    public void init() {
        LOGGER.info("Start to init zookeeper connection manager.");
        connect();
        reconnectExecutor = Executors.newSingleThreadExecutor(new NamedSequenceThreadFactory("ZKReconnect"));
        reconnFuture = reconnectExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000);
                    if (waitForReconnect && !zooKeeper.isPresent()) {
                        LOGGER.info("ZK reconnection is wanted.");
                        connect();
                    }
                } catch (InterruptedException e) {
                    LOGGER.info("Interrupted from sleep, return directly.");
                    return;
                } catch (Exception e) {
                    LOGGER.error("Unexpected error happens", e);
                }
            }

        });
    }

    private void connect() {
        try {
            ConnectionWatcher watcher = new ConnectionWatcher();
            zooKeeper = Optional.of(new ZooKeeper(connectStr, SESSION_TIMEOUT, watcher));
            watcher.waitUntilConnected();
        } catch (IOException e) {
            LOGGER.warn("Failed to create zookeeper connection.", e);
            zooKeeper = Optional.empty();
        }
    }

    /**
     * Destroy the zookeeper configuration manager
     */
    public void destroy() {
        LOGGER.info("Start to destroy zookeeper connection manager.");
        reconnFuture.cancel(false);
        reconnectExecutor.shutdownNow();
        try {
            if (!reconnectExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                LOGGER.warn("Failed to shutdown the reconnect monitor immediately.");
            }
        } catch (InterruptedException e) {
            LOGGER.warn("interrupted from await for termination");
        }
        zooKeeper.ifPresent(ZooKeeperUtil::closeNoException);
        zooKeeper = Optional.empty();
    }

    /**
     * get the zookeeper connection
     *
     * @return
     */
    public Optional<ZooKeeper> getConnection() {
        return zooKeeper;
    }

    /**
     * register the listener to monitor the connection status change
     *
     * @param listener
     */
    public void registerListener(ZKConnectionChangeWatcher listener) {
        listeners.add(listener);
    }

    private void notifyListener(ZKConnectionChangeWatcher.State state) {
        listeners.forEach(l -> l.stateChange(state));
    }


    private String getSessionId() {
        return zooKeeper.map(c -> "0x" + Long.toHexString(c.getSessionId())).orElse("NO-SESSION");
    }

    private class ConnectionWatcher implements Watcher {

        private final CountDownLatch latch = new CountDownLatch(1);
        private boolean connectionHasBeenEstablished = false;

        @Override
        public void process(WatchedEvent event) {
            Event.KeeperState state = event.getState();
            // Session expired, we only care for unexpected reasons
            // There is no automatic recovery from this, a new connection has to
            // be created
            if (state == Event.KeeperState.Expired) {
                LOGGER.error("The session [{}] in ZK has been expired will perform an automatic " +
                        "re-connection attempt", getSessionId());
                connect();
                if (!zooKeeper.isPresent() && !waitForReconnect) {
                    LOGGER.error("Failed to reconnect the zookeeper server");
                    waitForReconnect = true;
                }
            } else if (state == Event.KeeperState.SyncConnected) {
                // connection established
                LOGGER.info("Got connected event for session [{}] to zookeeper", getSessionId());
                if (connectionHasBeenEstablished) {
                    notifyListener(ZKConnectionChangeWatcher.State.RECONNECTED);
                } else {
                    notifyListener(ZKConnectionChangeWatcher.State.CONNECTED);
                }
                connectionHasBeenEstablished = true;
                waitForReconnect = false;
                latch.countDown();
            } else if (state == Event.KeeperState.Disconnected) {
                // connection lost
                LOGGER.warn("The session [{}] in ZooKeeper has lost its connection", getSessionId());
                notifyListener(ZKConnectionChangeWatcher.State.DISCONNECTED);
            }
        }

        private boolean waitUntilConnected() {
            try {
                return latch.await(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

}
