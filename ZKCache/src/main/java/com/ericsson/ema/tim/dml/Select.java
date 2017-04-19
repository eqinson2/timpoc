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
import static com.ericsson.ema.tim.lock.ZKCacheRWLockMap.zkCacheRWLock;
import static com.ericsson.ema.tim.reflection.MethodInvocationCache.AccessType.GET;
import static com.ericsson.ema.tim.reflection.Tab2MethodInvocationCacheMap.tab2MethodInvocationCacheMap;

public class Select implements Selector {
    private final static String TUPLE_FIELD = "records";
    private final List<Clause> clauses = new ArrayList<>();
    private final List<String> selectedFields;
    private String table;
    private TableInfoContext context;
    private List<Object> records;
    private MethodInvocationCache methodInvocationCache;

    private Select() {
        this.selectedFields = Collections.emptyList();
    }

    private Select(String... fields) {
        this.selectedFields = (fields == null || fields.length == 0) ? Collections.emptyList() : Arrays.asList(fields);
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
        this.table = tab;
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

    private void initExecuteContext() {
        this.context = tableInfoMap.lookup(table).orElseThrow(() -> new RuntimeException("Error: Selecting a " +
            "non-existing table:" + table));
        this.methodInvocationCache = tab2MethodInvocationCacheMap.lookup(table);

        //it is safe because records must be List according to JavaBean definition
        Object tupleField = invokeGetByReflection(context.getTabledata(), TUPLE_FIELD);
        assert (tupleField instanceof List<?>);
        //noinspection unchecked
        this.records = (List<Object>) tupleField;
    }

    private List<Object> internalExecute() {
        zkCacheRWLock.readLockTable(table);
        try {
            initExecuteContext();
            return records.stream().filter(
                r -> clauses.stream().map(c -> c.eval(r)).reduce(true, Boolean::logicalAnd))
                .collect(Collectors.toList());
        } finally {
            zkCacheRWLock.readUnLockTable(table);
        }
    }

    @Override
    public List<Object> execute() {
        if (!selectedFields.isEmpty())
            throw new RuntimeException("Must use executeWithSelectFields if some fields are to be selected");

        return internalExecute();
    }

    @Override
    public List<List<Object>> executeWithSelectFields() {
        if (selectedFields.isEmpty())
            throw new RuntimeException("Must use execute if full fields are to be selected");

        List<List<Object>> selectedResult = new ArrayList<>();
        for (Object obj : internalExecute()) {
            selectedResult.add(selectedFields.stream().map(field ->
                invokeGetByReflection(obj, field)).collect(Collectors.toList()));
        }
        return selectedResult;
    }
}

