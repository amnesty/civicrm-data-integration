package es.stratebi.civi.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.pentaho.di.core.row.ValueMetaInterface;

import java.util.HashMap;

/**
 */
public class CiviField {
    String fieldDef;
    String fieldName;
    String name;
    String title;
    String source = "getfields";
    Long type;

    final public static HashMap<Object, Integer> valueType = new HashMap<Object, Integer>();

    final public static HashMap<String, Long> dataTypeMapping = new HashMap<String, Long>();

    static {
        valueType.put(Long.valueOf(1), ValueMetaInterface.TYPE_INTEGER);
        valueType.put(Long.valueOf(2), ValueMetaInterface.TYPE_STRING);
        valueType.put(Long.valueOf(4), ValueMetaInterface.TYPE_DATE);
        valueType.put(Long.valueOf(12), ValueMetaInterface.TYPE_DATE);
        valueType.put(Long.valueOf(16), ValueMetaInterface.TYPE_INTEGER);
        valueType.put(Long.valueOf(32), ValueMetaInterface.TYPE_STRING);
        valueType.put(Long.valueOf(256), ValueMetaInterface.TYPE_DATE);

        // ---------- Custom fields
        valueType.put("Integer", ValueMetaInterface.TYPE_INTEGER);
        valueType.put("String", ValueMetaInterface.TYPE_STRING);
        valueType.put("Date", ValueMetaInterface.TYPE_DATE);
        valueType.put("Time", ValueMetaInterface.TYPE_DATE);


        dataTypeMapping.put("Integer", 1L);
        dataTypeMapping.put("String", 2L);
        dataTypeMapping.put("Date", 4L);
        dataTypeMapping.put("Time", 256L);
    }

    public CiviField() {
    }

    public CiviField(String jsonString) throws ParseException {
       JSONParser jsonParser = new JSONParser();
       JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);
    }

    public CiviField(String fieldName, JSONObject jsonObject) {
        this.setFieldName(fieldName);
        this.setFieldDef(jsonObject.toJSONString());
        this.setName((String) jsonObject.get("name"));
        if (jsonObject.get("title") != null)
            this.setTitle((String) jsonObject.get("title"));
        else if (jsonObject.get("label") != null)
            this.setTitle((String) jsonObject.get("label"));
        else this.setTitle("");

        Object type = jsonObject.get("type");
        if (type != null) {
            if (type.getClass().getSimpleName().equals("String"))
                this.setType(dataTypeMapping.get(jsonObject.get("type")));
            else
                this.setType((Long) type);
        } else if (jsonObject.get("data_type") != null)
            this.setType(dataTypeMapping.get(jsonObject.get("data_type")));
        else
            this.setType(dataTypeMapping.get("String"));
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public static HashMap<Object, Integer> getValueType() {
        return valueType;
    }

    public String getFieldDef() {
        return fieldDef;
    }

    public void setFieldDef(String fieldDef) {
        this.fieldDef = fieldDef;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getType() {
        return type;
    }

    public void setType(Long type) {
        this.type = type;
    }

    public int getMetaInterfaceType() {
        return valueType.get(this.type);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        CiviField clone = new CiviField();

        clone.setFieldName(getFieldName());
        clone.setTitle(getTitle());
        clone.setType(getType());
        clone.setFieldDef(getFieldDef());
        clone.setName(getName());
        return clone;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fieldDef", fieldDef);
        jsonObject.put("fieldName", fieldName);
        jsonObject.put("name", name);
        jsonObject.put("title", title);
        jsonObject.put("source", source);
        jsonObject.put("type", type);
        return jsonObject.toJSONString();
    }
}
