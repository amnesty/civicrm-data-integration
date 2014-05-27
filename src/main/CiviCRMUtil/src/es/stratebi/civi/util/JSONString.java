package es.stratebi.civi.util;

//import org.json.JSONObject;


public class JSONString {
    public Object jsonObject;
    public String jsonObjectKey;

    public long jsonIndex;

    public JSONString(Object jsonObject, long jsonIndex, String jsonObjectKey) {
        super();
        this.jsonObject = jsonObject;
        this.jsonIndex = jsonIndex;
        this.jsonObjectKey = jsonObjectKey;
    }
}
