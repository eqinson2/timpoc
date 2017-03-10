package com.ericsson.ema.tim.zookeeper;

public interface ZKConnectionChangeWatcher {

    enum State {
        CONNECTED, RECONNECTED, DISCONNECTED
    }

    /**
     * Signals a state change. <br>
     *
     * @param state
     *         The new state
     */
    void stateChange(State state);
}
