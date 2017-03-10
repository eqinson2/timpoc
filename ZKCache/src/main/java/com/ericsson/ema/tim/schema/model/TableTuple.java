package com.ericsson.ema.tim.schema.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by eqinson on 2017/3/7.
 */
public final class TableTuple extends NameType {
    //name = records, type = NaptrData
    private ArrayList<NameType> tuples;

    public TableTuple(String name, String type) {
        super(name, type);
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + "TableTuple{" +
                "tuples=" + tuples.stream().map(nt -> nt.toString() + "\n").collect(Collectors.joining
                ("\n")) +
                '}';
    }

    public List<NameType> getTuples() {
        if (tuples == null) {
            tuples = new ArrayList<>();
        }
        return tuples;
    }
}