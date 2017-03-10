package com.ericsson.ema.tim.schema;

import com.ericsson.ema.tim.schema.model.NameType;
import com.ericsson.ema.tim.schema.model.Table;
import com.ericsson.ema.tim.schema.model.TableTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.StringRenderer;

import java.util.Map;

public class XsdRender {
    private final static Logger LOGGER = LoggerFactory.getLogger(XsdRender.class);

    private static final String SCHEMA_TEMPALTE = "templates/schema.stg";

    private static String renderTemplate(ST template) {
        String output = template.render();
        LOGGER.debug("Generate output for template {} : \n{}", template.toString(), output);
        return output;
    }

    private static ST getTemplate(String name) {
        STGroup group = new STGroupFile(SCHEMA_TEMPALTE, '$', '$');
        group.registerRenderer(String.class, new StringRenderer());
        return group.getInstanceOf(name);
    }

    public static Table buildModelFromMetadata(String tableName, Map<String, String> metadata) {
        TableTuple tt = new TableTuple("records", tableName + "Data");
        metadata.forEach((k, v) -> tt.getTuples().add(new NameType(k, v)));
        return new Table(tableName, tt);
    }

    public static String renderXsd(Table table) {
        StringBuilder sb = new StringBuilder();

        ST stHeader = getTemplate("schemaHeader");
        sb.append(renderTemplate(stHeader));


        ST stTable = getTemplate("complexTypeForTable");
        stTable.add("ele", table);
        sb.append(renderTemplate(stTable));

        ST stTableData = getTemplate("complexTypeForTableTuple");
        stTableData.add("ele", table.getRecords());
        sb.append(renderTemplate(stTableData));

        ST stFoot = getTemplate("schemaFooter");
        sb.append(renderTemplate(stFoot));

        return sb.toString();
    }
}



