package com.ericsson.ema.tim.dml.condition;

import com.ericsson.ema.tim.dml.Select;

/**
 * Created by eqinson on 2017/3/7.
 */
public abstract class Clause {
    private Select parent;

    public Select getParent() {
        return parent;
    }

    public void setParent(Select parent) {
        this.parent = parent;
    }

    public abstract boolean eval(Object tuple);
}
