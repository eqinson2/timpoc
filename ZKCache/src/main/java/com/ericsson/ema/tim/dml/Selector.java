package com.ericsson.ema.tim.dml;

import com.ericsson.ema.tim.dml.condition.Clause;

import java.util.List;

/**
 * Created by eqinson on 2017/4/18.
 */
public interface Selector {
    Selector from(String tab);

    Selector where(Clause clause);

    List<Object> execute();

    List<List<Object>> executeWithSelectFields();
}
