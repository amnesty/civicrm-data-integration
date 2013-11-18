package es.stratebi.civi.util;

import org.json.JSONObject;


public class JSONString {
    public JSONObject jsonObject;
    public String jsonObjectKey;

    public long jsonIndex;

    public JSONString(JSONObject jsonObject, long jsonIndex, String jsonObjectKey) {
        super();
        this.jsonObject = jsonObject;
        this.jsonIndex = jsonIndex;
        this.jsonObjectKey = jsonObjectKey;
    }
}
