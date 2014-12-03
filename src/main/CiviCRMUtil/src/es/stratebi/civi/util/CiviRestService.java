package es.stratebi.civi.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CiviRestService {
    private String callUrl = "";
    private String restUrl = "";
    private String apiKey = "";
    private String key = "";
    private String action = "get";
    private String fieldsAction = "getfields";
    private String entity = "Contact";
    private String options = "";
    private int limit = 25;
    private long offset = 0;
    private boolean isFirst = true;
    private boolean isError = false;
    private boolean isDebug = false;
    private BufferedWriter sockedWriter;
    private BufferedReader socketReader;
    private Socket civiSocket;
    JSONParser parser = new JSONParser();
    private ArrayList<String> noInputActions = new ArrayList<String>();
    private ArrayList<String> noOutputActions = new ArrayList<String>();

    private HashMap<String, CiviField> entityFields = new HashMap<String, CiviField>();

    {
        this.noInputActions.add("getfields");
        this.noInputActions.add("getactions");
        this.noInputActions.add("create");
        this.noInputActions.add("update");
        this.noInputActions.add("replace");

        this.noInputActions.add("delete");                        // Verificar  -- Contact
        this.noInputActions.add("quicksearch");                   // Verificar  -- Contact
        this.noInputActions.add("activity_create");               // Verificar  -- Case
        this.noInputActions.add("merge");                         // Verificar  -- Contact
        this.noInputActions.add("transact");                      // Verificar  -- Contribution
        this.noInputActions.add("sendconfirmation");              // Verificar  -- Contribution
        this.noInputActions.add("install");                       // Verificar  -- Extension
        this.noInputActions.add("enable");                        // Verificar  -- Extension
        this.noInputActions.add("disable");                       // Verificar  -- Extension
        this.noInputActions.add("uninstall");                     // Verificar  -- Extension
        this.noInputActions.add("download");                      // Verificar  -- Extension
        this.noInputActions.add("refresh");                       // Verificar  -- Extension
        this.noInputActions.add("execute");                       // Verificar  -- Job
        this.noInputActions.add("geocode");                       // Verificar  -- Job
        this.noInputActions.add("send_reminder");                 // Verificar  -- Job
        this.noInputActions.add("update_greeting");               // Verificar  -- Job
        this.noInputActions.add("process_pledge");                // Verificar  -- Job
        this.noInputActions.add("process_mailing");               // Verificar  -- Job
        this.noInputActions.add("process_sms");                   // Verificar  -- Job
        this.noInputActions.add("fetch_bounces");                 // Verificar  -- Job
        this.noInputActions.add("fetch_activities");              // Verificar  -- Job
        this.noInputActions.add("process_participant");           // Verificar  -- Job
        this.noInputActions.add("process_membership");            // Verificar  -- Job
        this.noInputActions.add("process_respondent");            // Verificar  -- Job
        this.noInputActions.add("process_batch_merge");           // Verificar  -- Job
        this.noInputActions.add("run_payment_cron");              // Verificar  -- Job
        this.noInputActions.add("cleanup");                       // Verificar  -- Job
        this.noInputActions.add("disable_expired_relationships"); // Verificar  -- Job
        this.noInputActions.add("group_rebuild");                 // Verificar  -- Job
        this.noInputActions.add("mail_report");                   // Verificar  -- Job
        this.noInputActions.add("event_bounce");                  // Verificar  -- Mailing
        this.noInputActions.add("event_confirm");                 // Verificar  -- Mailing
        this.noInputActions.add("event_bounce");                  // Verificar  -- Mailing
        this.noInputActions.add("event_reply");                   // Verificar  -- Mailing
        this.noInputActions.add("event_forward");                 // Verificar  -- Mailing
        this.noInputActions.add("event_click");                   // Verificar  -- Mailing
        this.noInputActions.add("event_open");                    // Verificar  -- Mailing
        this.noInputActions.add("update_email_resetdate");        // Verificar  -- Mailing
        this.noInputActions.add("tree_get");                      // Verificar  -- Note
        this.noInputActions.add("set");                           // Verificar  -- Profile
        this.noInputActions.add("apply");                         // Verificar  -- Profile
        this.noInputActions.add("revert");                        // Verificar  -- Setting
        this.noInputActions.add("fill");                          // Verificar  -- Setting

        this.noOutputActions.add("get");
        this.noOutputActions.add("getfields");
        this.noOutputActions.add("getquick");
        this.noOutputActions.add("getactions");
        this.noOutputActions.add("getoptions");
        this.noOutputActions.add("getvalue");
        this.noOutputActions.add("getsingle");
        this.noOutputActions.add("getcount");
        this.noOutputActions.add("update");
        this.noOutputActions.add("replace");
    }

    private ArrayList<String> entityList = new ArrayList<String>();;
    private Long entityCount = 0L;
    private Boolean returnEmptyOnFail;
    private String civiJsonError = "";

    public CiviRestService(String restUrl, String apiKey, String key, String entity) {
        this.restUrl = restUrl;
        this.apiKey = apiKey;
        this.key = key;
        this.entity = entity;
    }

    public CiviRestService(String restUrl, String apiKey, String key, String action, String entity) {
        this.restUrl = restUrl;
        this.apiKey = apiKey;
        this.key = key;
        this.action = action;
        this.entity = entity;
    }

    public String getRestUrl() {
        return restUrl;
    }

    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFieldsAction() {
        return fieldsAction;
    }

    public void setFieldsAction(String fieldsAction) {
        this.fieldsAction = fieldsAction;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean isFirst) {
        this.isFirst = isFirst;
    }

    public HashMap<String, CiviField> getEntityFields() {
        return entityFields;
    }

    public void setEntityFields(HashMap<String, CiviField> entityFields) {
        this.entityFields = entityFields;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean isError) {
        this.isError = isError;
    }

    public HttpURLConnection getHttpConnection(String method, URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("HTTP error: " + conn.getResponseCode());
        }

        return conn;
    }

    public String getCiviResponse(HttpURLConnection conn) throws IOException {
        String response = "";

        InputStream input = conn.getInputStream();
        InputStreamReader streamReader = new InputStreamReader(input);
        BufferedReader br = new BufferedReader(streamReader);

        String line = "";

        while ((line = br.readLine()) != null) {
            response = response.concat(line);
        }

        return response;
    }

    public URL getUrl(String entity, String action, String params) throws MalformedURLException {
        String urlString = "";

        urlString = restUrl + "?api_key=".concat(apiKey);
        urlString = urlString + "&key=".concat(key);

        urlString = urlString.concat("&json=1");
        urlString = urlString.concat("&debug=0");
        urlString = urlString.concat("&version=3");

        urlString = urlString + "&entity=".concat(entity);
        urlString = urlString + "&action=".concat(action);

        urlString = urlString + params;

        URL url = new URL(urlString);

        return url;
    }

    public boolean isValidJSONObject(String json) {
        boolean valid;

        try {
            Object obj = parser.parse(json);
            //new JSONObject(json);
            valid = true;
        } catch (ParseException e) {
            valid = false;
            e.printStackTrace();
        }

        return valid;
    }

    public HashMap<String, CiviField> getFieldList(boolean reset) throws IOException, CiviCRMException {
        if (reset)
            getFields();

        return entityFields;
    }

    private void getFields() throws IOException, CiviCRMException {
        try {
            String params = "&options[limit]=0";
            getEntityList(false);

            if (!entityList.contains(this.entity)) {
                throw new CiviCRMException("CiviCRM Error: Entity [" + this.entity + "] not exists ");
            } else {
                URL url = getUrl(this.entity, this.fieldsAction, params);
                HttpURLConnection conn = getHttpConnection("GET", url);
                String json = getCiviResponse(conn);

                JSONObject jsonObject = (JSONObject) parser.parse(json);

                // Aqui debo chequear si hay un error de acceso
                if ((Long)jsonObject.get("is_error") == 1L) {
                    throw new CiviCRMException("CiviCRM API Error: " + jsonObject.get("error_message").toString());
                }
                //else if ((Long)jsonObject.get("is_error") == 0L && (Long)jsonObject.get("count") == 0L) {
                //}
                else {
                    entityFields = new HashMap<String, CiviField>();
                    Object values = jsonObject.get("values");
                    if (values.getClass().getSimpleName().equals("JSONObject")) {
                        JSONObject jsonFields = (JSONObject)values;
                        for (Object key : jsonFields.keySet()) {
                            String fieldName = (String) key;
                            JSONObject jsonField = (JSONObject) jsonFields.get(key);
                            try {
                                entityFields.put(fieldName, new CiviField(fieldName, jsonField));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (values.getClass().getSimpleName().equals("JSONArray")) {
//                        JSONObject jsonField = new JSONObject();
//                        jsonField.put("name", this.entity);
//                        jsonField.put("type", "String");
//                        entityFields.put(this.entity, new CiviField(this.entity, jsonField));
                        /*
                         * Entity: ActivityType            fieldsArray size: 0
                         * Entity: CustomSearch            fieldsArray size: 0
                         * Entity: Entity                  fieldsArray size: 0
                         * Entity: Location                fieldsArray size: 0
                         * Entity: MailingEventConfirm     fieldsArray size: 0
                         * Entity: MailingEventResubscribe fieldsArray size: 0
                         * Entity: MailingEventSubscribe   fieldsArray size: 0
                         * Entity: MailingEventUnsubscribe fieldsArray size: 0
                         * Entity: PriceField              fieldsArray size: 0
                         * Entity: PriceFieldValue         fieldsArray size: 0
                         * Entity: PriceSet                fieldsArray size: 0
                         * Entity: ReportTemplate          fieldsArray size: 0
                         * Entity: SurveyRespondant        fieldsArray size: 0
                         * Entity: System                  fieldsArray size: 0
                         */
                    }

                    // Llamar al get de la entidad limitado a un registro para ver que campos devuelve
                    // y eliminar de entityFields todos aquellos que no esten en el resultado
                    // y añadir los que no estén. En ActivityType no hay campos pero si hay un resultado
                    // updateReturningFields();
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
    }

    public void updateReturningFields(HashMap<String, CiviField> fieldListHashMap, HashMap<String, CiviField> filterFieldHashMap) throws IOException, CiviCRMException, ParseException {
        ArrayList<String> entityKeyList = new ArrayList<String>();
        ArrayList<String> extraFieldList = new ArrayList<String>();
        String params = "&options[limit]=1&options[offset]=1";

        URL url = getUrl(entity, "get", params);
        HttpURLConnection conn = getHttpConnection("GET", url);

        String jsonResponse = getCiviResponse(conn);

        JSONObject jsonObject = (JSONObject)parser.parse(jsonResponse);

        if ((Long)jsonObject.get("is_error") == 1L) {
            throw new CiviCRMException("CiviCRM API Error: " + jsonObject.get("error_message").toString());
        } else {
            Object valueField = jsonObject.get("values");
            Object fieldValue;

            if(valueField.getClass().getSimpleName().equals("JSONObject")) {
                JSONObject fieldSet = (JSONObject) valueField;
                //boolean is_rowset = false;
                for (Object key: fieldSet.keySet()) {
                    HashMap<String, Object> fieldValues = new HashMap<String, Object>();
                    Object field = fieldSet.get(key);
                    if (field.getClass().getSimpleName().equals("JSONObject")) {
                        // MailSettings
                        // {"object1":{"key1":"Value1","key2":"Value2", ...},"object2":{"key1":"Value1","key2":"Value2", ...},...}
                        JSONObject jsonField = (JSONObject) field;
                        for (Object fieldName: jsonField.keySet()) {
                            entityKeyList.add((String) fieldName);
                        }

                    } else if (field.getClass().getSimpleName().equals("String")) {
                        // ActivityType
                        // {"id1":"Value1","id2":"Value2", ...}
                        entityKeyList.add("ActivityTypeId");
                        entityKeyList.add((String) this.entity);
                    }
                }
            } else if (valueField.getClass().getSimpleName().equals("JSONArray")) {
                JSONArray values = (JSONArray)jsonObject.get("values");

                Iterator<Object> iterator = values.iterator();

                while (iterator.hasNext()) {
                    Object entityObject = iterator.next();
                    HashMap<String, Object> fieldValues = new HashMap<String, Object>();

                    if (entityObject.getClass().getSimpleName().equals("JSONObject")) {
                        // File
                        // [{"key1":"Value1","key2":"Value2", ...},{"key1":"Value1","key2":"Value2", ...},...]
                        JSONObject jsonArrayField = (JSONObject) entityObject;
                        for (Object fieldName: jsonArrayField.keySet()) {
                            entityKeyList.add((String) this.entity);
                        }
                    } else if (entityObject.getClass().getSimpleName().equals("String")) {
                        // Entity
                        // ["Activity","ActivityType","Address", ...]
                        entityKeyList.add("Entity");
                    }
                }
            }
        }

        extraFieldList.addAll(entityKeyList);
        filterFieldHashMap.clear();
        filterFieldHashMap.putAll(fieldListHashMap);

        HashMap<String, CiviField> tmpFieldsMap = new HashMap<String, CiviField>();
        tmpFieldsMap.putAll(fieldListHashMap);
        // Obteniendo listado de campos del getfields no devueltos en el get
        for(String field: entityKeyList) {
            tmpFieldsMap.remove(field);
        }

        // Obteniendo listado de campos del get no presentes en el getfields
        for(String field: fieldListHashMap.keySet()) {
            extraFieldList.remove(field);
        }

        // Eliminando campos del getfields no devueltos en el get en la lista de campos de salida
        for(String field: tmpFieldsMap.keySet()) {
            fieldListHashMap.remove(field);
        }

        // Añadiendo nuevos campos devueltos en el get y no presentes en el getFields
        // en la lista de campos de salida y en la lista de filtros
        for(String fieldName: extraFieldList) {
            CiviField civiField = new CiviField();
            civiField.setName(fieldName);
            civiField.setFieldName(fieldName);
            civiField.setTitle("");
            civiField.setSource("get");
            if (fieldName.endsWith("_id"))
                civiField.setType(CiviField.dataTypeMapping.get("Integer"));
            else
                civiField.setType(CiviField.dataTypeMapping.get("String"));

            fieldListHashMap.put(fieldName, civiField);
            filterFieldHashMap.put(fieldName, civiField);
        }
    }

    public ArrayList<String> getEntityActions(boolean input) throws IOException, CiviCRMException {
        ArrayList<String> actions = new ArrayList<String>();
        try {
            String params = "&options[limit]=0";

            URL url = getUrl(this.entity, this.action, params);
            HttpURLConnection conn = getHttpConnection("GET", url);
            String json = getCiviResponse(conn);

            JSONObject jsonObject = (JSONObject) parser.parse(json);

            // Aqui debo chequear si hay un error de acceso
            if ((Long)jsonObject.get("is_error") == 1L) {
                throw new CiviCRMException("CiviCRM API Error: " + jsonObject.get("error_message").toString());
            } else if ((Long)jsonObject.get("is_error") == 0L && (Long)jsonObject.get("count") == 0L) {
                throw new CiviCRMException("CiviCRM Error: Entity [" + this.entity + "] has not fields or not exists ");
            } else {
                JSONArray values = (JSONArray) jsonObject.get("values");
                actions.addAll(values.subList(0, values.size()));

                if (input) {
                    actions.removeAll(this.noInputActions);
                    if (!actions.contains("get")) {
                        actions.add(0, "get");
                    }
                } else {
                    actions.removeAll(this.noOutputActions);
                }
                HashSet<String> set = new HashSet<String>(actions);
                String stringArray[] = new String[0];
                stringArray = set.toArray(stringArray);
                Arrays.sort(stringArray);
                actions.clear();
                actions.addAll(Arrays.asList(stringArray));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        return actions;
    }

    public ArrayList<String> getEntityList() throws IOException, ParseException, CiviCRMException {
        return getEntityList(true);
    }

    public ArrayList<String> getEntityList(boolean refresh) throws IOException, ParseException, CiviCRMException {
        if (entityList.size() == 0 || refresh) {
            entityList = new ArrayList<String>();
            URL url = getUrl("Entity", "get", "");
            HttpURLConnection conn = getHttpConnection("GET", url);
            String jsonString = getCiviResponse(conn);

            JSONObject jsonObject = (JSONObject) parser.parse(jsonString);

            // Aqui debo chequear si hay un error de acceso
            if ((Long) jsonObject.get("is_error") == 1L) {
                throw new CiviCRMException("CiviCRM API Error: " + jsonObject.get("error_message").toString());
            } else {
                JSONArray values = (JSONArray) jsonObject.get("values");

                Iterator<String> valueIterator = values.iterator();
                String entityName;
                // take each value from the json array separately
                while (valueIterator.hasNext()) {
                    entityName = valueIterator.next();
                    entityList.add(entityName);
                }
            }
        }
        return entityList;
    }

    public Long getRowCount(String params) throws IOException, CiviCRMException, ParseException {
        if (!this.isFirst && (this.action.equals("getoptions") || this.action.equals("getvalue") || this.action.equals("getsingle"))) {
            return 0L;
        } else if (this.isFirst) {
            if (this.action.equals("getoptions") || this.action.equals("getvalue") || this.action.equals("getsingle")) {
                return 1L;
            } else {
                URL url = getUrl(entity, "getcount", params);
                HttpURLConnection conn = getHttpConnection("GET", url);
                this.callUrl = conn.getRequestMethod() + ": " + url.toString();

                String jsonResponse = getCiviResponse(conn);
                JSONObject jsonObject = (JSONObject) parser.parse(jsonResponse);

                // Aqui debo chequear si hay un error de acceso
                if ((Long) jsonObject.get("is_error") == 1L) {
                    throw new CiviCRMException("CiviCRM API Error: " + jsonObject.get("error_message").toString());
                } else {
                    this.entityCount = Long.valueOf(jsonObject.get("result").toString());
                }
            }
        }
        return this.entityCount;
    }

    public List<HashMap<String, Object>> getNextValues() throws IOException, CiviCRMException, ParseException {
        this.offset = (this.isFirst ? 0 : this.offset + this.limit);
        ArrayList<HashMap<String, Object>> returnValueList = new ArrayList<HashMap<String, Object>>();
        String params = (this.options.equals("") ? "" : this.options);
        if (this.offset >= getRowCount(params)) {
            return returnValueList;
        }
        this.isFirst = false;

        if (this.action.equals("getsingle") || this.action.equals("getvalue") || this.action.equals("getcount") || this.action.equals("getoptions")) {
            if (this.offset > 1) {
                // No hacer la llamada si la accion es getsingle pues se devuelve un solo registro
                // en la primera llamada,en las siguientes genera un error
                return returnValueList;
            }
        }

        params += "&options[limit]=" + this.limit
                + "&options[offset]=" + this.offset
                + (isDebug ? "&debug=1" : "");

        URL url = getUrl(entity, action, params);
        HttpURLConnection conn = getHttpConnection("GET", url);
        this.callUrl = conn.getRequestMethod() + ": " + url.toString();

        String jsonResponse = getCiviResponse(conn);

        JSONObject jsonObject = (JSONObject)parser.parse(jsonResponse);

        // Aqui debo chequear si hay un error de acceso
        if ((Long)jsonObject.get("is_error") == 1L) {
            this.isError = true;
            this.civiJsonError = jsonResponse;
            if (returnEmptyOnFail)
                return returnValueList;
            else
                throw new CiviCRMException("CiviCRM API Error: " + jsonObject.get("error_message").toString());
        } else {
            isError = false;
            this.civiJsonError = "";
                    Object valueField = jsonObject.get("values");
            Object fieldValue;

            if (valueField == null) {
                // getsingle, getvalue, getcount
                // {"key1":"Value1","key2":"Value2", ...}

                HashMap<String, Object> fieldValues = new HashMap<String, Object>();
                for (Object fieldName: jsonObject.keySet()) {
                    if (!fieldName.equals("is_error")) {
                        fieldValue = jsonObject.get(fieldName);
                        fieldValues.put((String) fieldName, fieldValue);
                    }
                }
                returnValueList.add(fieldValues);
            } else if (action.equals("getoptions")) {
                // getoptions
                // {"key1":"Value1","key2":"Value2", ...}
                JSONObject fieldSet = (JSONObject) valueField;

                for (Object fieldName: fieldSet.keySet()) {
                    HashMap<String, Object> fieldValues = new HashMap<String, Object>();
                    fieldValue = fieldSet.get(fieldName);
                    fieldValues.put("key", fieldName);
                    fieldValues.put("value", fieldValue);
                    returnValueList.add(fieldValues);
                }
            } else if(valueField.getClass().getSimpleName().equals("JSONObject")) {
                JSONObject fieldSet = (JSONObject) valueField;
                //boolean is_rowset = false;
                for (Object key: fieldSet.keySet()) {
                    HashMap<String, Object> fieldValues = new HashMap<String, Object>();
                    Object field = fieldSet.get(key);
                    if (field.getClass().getSimpleName().equals("JSONObject")) {
                        // MailSettings
                        // {"object1":{"key1":"Value1","key2":"Value2", ...},"object2":{"key1":"Value1","key2":"Value2", ...},...}
                        JSONObject jsonField = (JSONObject) field;
                        for (Object fieldName: jsonField.keySet()) {
                            fieldValue = jsonField.get(fieldName);
                            fieldValues.put((String)fieldName, fieldValue);
                        }

                    } else if (field.getClass().getSimpleName().equals("String")) {
                        // ActivityType
                        // {"id1":"Value1","id2":"Value2", ...}
                        fieldValue = field;
                        fieldValues.put("ActivityTypeId", key); //(String)key
                        fieldValues.put(this.entity, fieldValue); //(String)key
                    }
                    returnValueList.add(fieldValues);
                }
            } else if (valueField.getClass().getSimpleName().equals("JSONArray")) {
                JSONArray values = (JSONArray)jsonObject.get("values");

                Iterator<Object> iterator = values.iterator();

                while (iterator.hasNext()) {
                    Object entityObject = iterator.next();
                    HashMap<String, Object> fieldValues = new HashMap<String, Object>();

                    if (entityObject.getClass().getSimpleName().equals("JSONObject")) {
                        // File
                        // [{"key1":"Value1","key2":"Value2", ...},{"key1":"Value1","key2":"Value2", ...},...]
                        JSONObject jsonArrayField = (JSONObject) entityObject;
                        for (Object fieldName: jsonArrayField.keySet()) {
                            fieldValue = jsonArrayField.get(fieldName);
                            fieldValues.put((String)fieldName, fieldValue);
                        }
                    } else if (entityObject.getClass().getSimpleName().equals("String")) {
                        // Entity
                        // ["Activity","ActivityType","Address", ...]
                        fieldValue = entityObject;
                        fieldValues.put((String)key, fieldValue);
                    }
                    returnValueList.add(fieldValues);
                }
            }
        }
        return returnValueList;
    }

    public String[] preparePostUrlString(String entity, String action, String params) {
        String[] preparedUrl = new String[2];

        preparedUrl[0] = restUrl;

        preparedUrl[1] = "api_key=".concat(apiKey);
        preparedUrl[1] = preparedUrl[1] + "&key=".concat(key);

        preparedUrl[1] = preparedUrl[1].concat("&json=1");
        preparedUrl[1] = preparedUrl[1].concat("&debug=1");
        preparedUrl[1] = preparedUrl[1].concat("&version=3");

        preparedUrl[1] = preparedUrl[1] + "&entity=".concat(entity);
        preparedUrl[1] = preparedUrl[1] + "&action=".concat(action);

        preparedUrl[1] = preparedUrl[1] + params;

        return preparedUrl;
    }

    public String putValues(String params, String filters, boolean closeConnection) {
        boolean error = false;
        String msg = "";
        try {
            // Ver lor filtros para la actualizacion
            String apiUrlString[] = preparePostUrlString(entity, action, params);
      /*
       * Estos dos metodos hacen lo mismo, depende del gusto el que se use, creo
       * que es mejor usando socket pues es mas simple tener la traza completa
       * para depurar y mantener la conexion abierta hasta finalizar
       */

            msg = tryPostWithUrl(apiUrlString);
            // String result = tryPostWithSocket(apiUrlString, closeConnection);
            // Si aparece la cadena "error_code" se ha producido un error, hay que
            // verificar otros
            // casos con la variable is_error

        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    public void closeSocketConnection() {
        try {
            if (socketReader != null) {
                socketReader.close();
            }
            if (sockedWriter != null) {
                sockedWriter.close();
            }
            if (civiSocket != null && !civiSocket.isClosed()) {
                civiSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String tryPostWithSocket(String params[], boolean closeConnection) {
        String postResult = "";
        try {
            // Construct data
            String data = params[1];
            URL url = new URL(params[0]);

            // Create a socket to the host
            String hostname = url.getHost();
            int port = url.getPort() == -1 ? 80 : url.getPort();
            InetAddress addr = InetAddress.getByName(hostname);
            if (!closeConnection && (civiSocket == null || civiSocket.isClosed())) {
                civiSocket = new Socket(addr, port);
            }

            // Send header
            String path = url.getPath();
            if (!closeConnection && sockedWriter == null) {
                sockedWriter = new BufferedWriter(new OutputStreamWriter(civiSocket.getOutputStream(), "UTF8"));
            }

            sockedWriter.write("POST " + path + " HTTP/1.0\r\n");
            sockedWriter.write("Content-Length: " + data.length() + "\r\n");
            sockedWriter.write("Content-Type: application/x-www-form-urlencoded\r\n");
            sockedWriter.write("\r\n");

            sockedWriter.write(data);
            sockedWriter.flush();

            // Get the response
            if (!closeConnection && socketReader == null) {
                socketReader = new BufferedReader(new InputStreamReader(civiSocket.getInputStream()));
            }
            String line;
            while ((line = socketReader.readLine()) != null) {
                postResult += line + "\n";
            }

            if (closeConnection) {
                sockedWriter.close();
                socketReader.close();
                civiSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return postResult;
    }

    public String tryPostWithUrl(String params[]) {
        String postResult = "";
        try {
            // Construct data
            String data = params[1];
            URL url = new URL(params[0]);

            data = data + (isDebug ? "&debug=1" : "");
            this.callUrl = "POST: " + url.toString() + "\nSend data: " + data;

            // Send data
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

            wr.write(data);
            wr.flush();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                postResult += line + "\n";
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return postResult;
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public String getCallUrl() {
        return callUrl;
    }

    public void setCallUrl(String callUrl) {
        this.callUrl = callUrl;
    }

    public boolean getIsDebug() {
        return isDebug;
    }

    public String getCiviJsonError() {
        return civiJsonError;
    }

    public void setReturnEmptyOnFail(Boolean returnEmptyOnFail) {
        this.returnEmptyOnFail = returnEmptyOnFail;
    }

    public Boolean getReturnEmptyOnFail() {
        return returnEmptyOnFail;
    }
}
