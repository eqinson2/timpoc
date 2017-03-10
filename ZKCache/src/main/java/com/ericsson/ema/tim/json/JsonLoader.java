package com.ericsson.ema.tim.json;

import com.ericsson.ema.tim.json.model.FieldInfo;
import com.ericsson.ema.tim.json.model.TypeInfo;
import com.ericsson.ema.tim.utils.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonLoader {
    private final static String TABLE_TAG = "Table";
    private final static String ID_TAG = "Id";
    private final static String TABLE_HEADER_TAG = "Header";
    private final static String TABLE_CONTENT_TAG = "Content";
    private final static String TABLE_TUPLE_TAG = "Tuple";
    private final static Pattern p = Pattern.compile("\\{[\\w ]+\\}");

    private final Logger LOGGER = LoggerFactory.getLogger(JsonLoader.class);
    private String tableName;
    private Map<Integer, TypeInfo> tableHeaderIndexMap = new HashMap<>();
    private Map<String, String> tableMetadata = new LinkedHashMap<>();
    private List<List<FieldInfo>> tupleList = new ArrayList<>();

    public JsonLoader() {
    }

    public JsonLoader(String tableName) {
        this.tableName = tableName;
    }

    private static String trimBrace(String s) {
        if (s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        } else {
            return "";
        }
    }

    private void parseTableHeader(JSONObject root) {
        JSONArray arr = root.getJSONArray(TABLE_HEADER_TAG);
        for (int i = 0; i < arr.length(); i++) {
            Iterator<String> keys = arr.getJSONObject(i).keys();
            while (keys.hasNext()) {
                String key = keys.next();
                tableHeaderIndexMap.put(i, new TypeInfo(key, (String) arr.getJSONObject(i).get(key)));
                tableMetadata.put(key, (String) arr.getJSONObject(i).get(key));
            }
        }
    }

    private void parseTableContent(JSONObject root) {
        JSONArray arr = root.getJSONArray(TABLE_CONTENT_TAG);
        for (int i = 0; i < arr.length(); i++) {
            String content = arr.getJSONObject(i).getString(TABLE_TUPLE_TAG);
            List<FieldInfo> tuple = new ArrayList<>();
            Matcher m = p.matcher(content);

            int colume = 0;
            while (m.find()) {
                tuple.add(new FieldInfo(trimBrace(m.group()), tableHeaderIndexMap.get(colume).getName(),
                        tableHeaderIndexMap.get(colume).getType()));
                colume++;
            }
            tupleList.add(tuple);
        }
    }

    public void loadJsonFromString(String jsonStr) {
        JSONObject obj = new JSONObject(jsonStr);
        JSONObject table = obj.getJSONObject(TABLE_TAG);

        if (tableName == null)
            tableName = table.getString(ID_TAG);

        parseTableHeader(table);

        if (LOGGER.isDebugEnabled()) {
            tableHeaderIndexMap.forEach((k, v) -> LOGGER.debug("key : {}, value: {}", k, v));
            tableMetadata.forEach((k, v) -> LOGGER.debug("key : {}, value: {}", k, v));
        }

        parseTableContent(table);

        if (LOGGER.isDebugEnabled()) {
            tupleList.forEach(t -> {
                t.forEach(f -> LOGGER.debug("field info: {}", f));
            });
        }
    }

    public void loadJsonFromFile(String jsonFile) throws IOException {
        String jsonStr = FileUtils.readFile(jsonFile);
        loadJsonFromString(jsonStr);
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, String> getTableMetadata() {
        return tableMetadata;
    }

    public List<List<FieldInfo>> getTupleList() {
        return tupleList;
    }
}


