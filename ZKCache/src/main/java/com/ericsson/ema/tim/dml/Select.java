package com.ericsson.ema.tim.dml;

import com.ericsson.ema.tim.dml.condition.Clause;
import com.ericsson.ema.tim.reflection.MethodInvocationCache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.ericsson.ema.tim.dml.TableInfoMap.tableInfoMap;
import static com.ericsson.ema.tim.lock.GlobalRWLock.globalRWLock;
import static com.ericsson.ema.tim.reflection.MethodInvocationCache.AccessType.GET;
import static com.ericsson.ema.tim.reflection.Tab2MethodInvocationCacheMap.tab2MethodInvocationCacheMap;

public class Select implements Selector {
    private final static String TUPLE_FIELD = "records";
    private final List<Clause> clauses = new ArrayList<>();
    private final List<String> selectedFields;
    private TableInfoContext context;
    private List<Object> records;
    private MethodInvocationCache methodInvocationCache;

    private Select() {
        this.selectedFields = Collections.emptyList();
    }

    private Select(String... fields) {
        this.selectedFields = (fields == null || fields.length == 0) ?
            Collections.emptyList() : Arrays.asList(fields);
    }

    public static Selector select() {
        return new Select();
    }

    public static Selector select(String... fields) {
        return new Select(fields);
    }

    public MethodInvocationCache getMethodInvocationCache() {
        return methodInvocationCache;
    }

    public TableInfoContext getContext() {
        return context;
    }

    @Override
    public Selector from(String tab) {
        this.methodInvocationCache = tab2MethodInvocationCacheMap.lookup(tab);
        this.context = tableInfoMap.lookup(tab).orElseThrow(
            () -> new RuntimeException("No such table:" + tab));
        return from(context.getTabledata());
    }

    private Selector from(Object obj) {
        //it is safe because records must be List according to JavaBean definition
        Object tupleField = invokeGetByReflection(obj, TUPLE_FIELD);
        assert (tupleField instanceof List<?>);
        //noinspection unchecked
        this.records = (List<Object>) tupleField;
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

    @Override
    public Selector where(Clause clause) {
        this.clauses.add(clause);
        clause.setParent(this);
        return this;
    }

    private List<Object> internalExecute() {
        globalRWLock.readLock();
        try {
            return records.stream().filter(
                r -> clauses.stream().map(c -> c.eval(r)).reduce(true, Boolean::logicalAnd))
                .collect(Collectors.toList());
        } finally {
            globalRWLock.readUnlock();
        }
    }

    @Override
    public List<Object> execute() {
        if (context == null)
            throw new RuntimeException("Table not specified in Select");
        else if (!selectedFields.isEmpty())
            throw new RuntimeException("Must use executeWithSelectFields if some fields are to be selected");
        else return internalExecute();
    }

    @Override
    public List<List<Object>> executeWithSelectFields() {
        if (context == null)
            throw new RuntimeException("Table not specified in Select");
        else if (selectedFields.isEmpty())
            throw new RuntimeException("Must use execute if full fields are to be selected");

        List<List<Object>> selectedResult = new ArrayList<>();
        for (Object obj : internalExecute()) {
            selectedResult.add(selectedFields.stream().map(field ->
                invokeGetByReflection(obj, field)).collect(Collectors.toList()));
        }
        return selectedResult;
    }
}

