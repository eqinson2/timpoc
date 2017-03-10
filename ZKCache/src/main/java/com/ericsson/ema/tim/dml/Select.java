package com.ericsson.ema.tim.dml;

import com.ericsson.ema.tim.dml.condition.Clause;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.ericsson.ema.tim.dml.TableInfoMap.tableInfoMap;
import static com.ericsson.ema.tim.reflection.MethodInvocationCache.AccessType.GET;
import static com.ericsson.ema.tim.reflection.MethodInvocationCache.methodInvocationCache;

public class Select {
    private final static String TUPLE_FIELD = "records";
    private TableInfoContext context;
    private List<Clause> clauses = new ArrayList<>();
    private List<Object> records;
    private List<String> selectedFields;

    private Select() {
        this.selectedFields = Collections.emptyList();
    }

    private Select(String... fields) {
        this.selectedFields = (fields == null || fields.length == 0) ?
                Collections.emptyList() : Arrays.asList(fields);
    }

    public static Select select() {
        return new Select();
    }

    public static Select select(String... fields) {
        return new Select(fields);
    }

    public TableInfoContext getContext() {
        return context;
    }

    public Select from(String tab) {
        context = tableInfoMap.lookup(tab);
        if (context != null) {
            return from(context.getTabledata());
        } else {
            throw new RuntimeException("No such table:" + tab);
        }
    }

    private Select from(Object obj) {
        //it is safe because records must be List according to JavaBean definition
        Object tupleField = invokeGetByReflection(obj, TUPLE_FIELD);
        assert (tupleField instanceof List<?>);
        records = (List<Object>) tupleField;
        return this;
    }

    private Object invokeGetByReflection(Object obj, String wantedField) {
        Method getter = methodInvocationCache.get(obj.getClass(), wantedField, GET);
        try {
            return getter.invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage());//should never happen
        }
    }

    public Select where(Clause clause) {
        this.clauses.add(clause);
        clause.setParent(this);
        return this;
    }

    public List<Object> execute() {
        List<Object> result = records.stream().filter(
                o -> clauses.stream()
                        .map(eachClause -> eachClause.eval(o))
                        .reduce(true, (c, clauseResult) -> c && clauseResult))
                .collect(Collectors.toList());

        if (selectedFields.isEmpty()) {
            return result;
        } else {
            List<Object> selectedResult = new ArrayList<>();
            for (Object obj : result) {
                selectedResult.add(selectedFields.stream().map(field ->
                        invokeGetByReflection(obj, field)).collect(Collectors.toList()));
            }
            return selectedResult;
        }
    }
}

