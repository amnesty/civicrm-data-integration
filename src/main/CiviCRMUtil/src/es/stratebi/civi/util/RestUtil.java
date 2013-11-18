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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RestUtil {
    private String restUrl = "";
    private String apiKey = "";
    private String key = "";
    private String action = "get";
    private String fieldsAction = "getfields";
    private String entity = "Contact";
    private String options = "";
    private int limit = 25;
    private int offset = 0;
    private boolean isFirst = true;
    private boolean isError = false;
    private BufferedWriter sockedWriter;
    private BufferedReader socketReader;
    private Socket civiSocket;

    private HashMap<String, FieldAttrs> aFields = new HashMap<String, FieldAttrs>();

    public RestUtil(String restUrl, String apiKey, String key, String entity) {
        this.restUrl = restUrl;
        this.apiKey = apiKey;
        this.key = key;
        this.entity = entity;
    }

    public RestUtil(String restUrl, String apiKey, String key, String action, String entity) {
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

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getOffset() {
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

	public HashMap<String, FieldAttrs> getaFields() {
		return aFields;
	}

	public void setaFields(HashMap<String, FieldAttrs> aFields) {
		this.aFields = aFields;
	}

	public boolean isError() {
		return isError;
	}

	public void setError(boolean isError) {
		this.isError = isError;
	}

    public HttpURLConnection getHttpConnection(URL url) throws IOException {
    	HttpURLConnection conn =  (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("HTTP error: " + conn.getResponseCode());
        }
    	
        return conn;
    }

    public String getJSonString(HttpURLConnection conn) throws IOException {
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
            new JSONObject(json);
            valid = true;
        } catch (JSONException e) {
            valid = false;
        }

        return valid;
    }

    public boolean isValidJSONArray(String json) {
        boolean valid;

        try {
            new JSONArray(json);
            valid = true;
        } catch (JSONException e) {
            valid = false;
        }

        return valid;
    }

    public HashMap<String, FieldAttrs> getFieldLists(boolean reset) throws IOException, CiviCRMException {
        if (reset) getFields();
        
        return aFields;
    }

    private void getFields() throws IOException, CiviCRMException {
        try {
            String params = "&options[limit]=0";

            URL url = getUrl(this.entity, this.fieldsAction, params);
			HttpURLConnection conn = getHttpConnection(url);
			String json = getJSonString(conn);

            assert isValidJSONObject(json) : "String is not a valid JSON.";

            JSONObject response = new JSONObject(json);

        	// Aqui debo chequear si hay un error de acceso
        	if (response.get("is_error").toString().equals("1")) {
        		throw new CiviCRMException("CiviCRM API Error: " + response.get("error_message").toString());
        	} else
    		if (response.get("is_error").toString().equals("0") && response.get("count").toString().equals("0")) {
        		throw new CiviCRMException("CiviCRM Error: Entity [" + this.entity + "] has not fields or not exists ");
        	} else
            if (isValidJSONObject(response.get("values").toString())) {
                JSONObject values = response.getJSONObject("values");

                @SuppressWarnings("unchecked")
                Iterator<String> fieldIterator = values.keys();

                JSONObject jsonField = null;

                while (fieldIterator.hasNext()) {
                    String fieldName = fieldIterator.next();
                    jsonField = values.getJSONObject(fieldName);

                    @SuppressWarnings("unchecked")
                    Iterator<String> fieldsIterator = jsonField.keys();
                    FieldAttrs cf = new FieldAttrs();
                    cf.setfFieldKey(fieldName);

                    while (fieldsIterator.hasNext()) {
                        String fName = fieldsIterator.next();
                        String fValue = jsonField.get(fName).toString();

                        try {
                            cf.set("f" + fName.substring(0, 1).toUpperCase() + fName.substring(1).replace(".", "_"), fValue);
                        } catch (Exception e) {
                            // e.printStackTrace();
                        }
                    }
                    aFields.put(fieldName, cf);
                }
            } else if (isValidJSONArray(response.get("values").toString())) {
                JSONArray values = response.getJSONArray("values");

                for (int i = 0; i <= values.length() - 1; i++) {
                    //System.out.println(values.get(i).toString());
                }
            } else {
                System.out.println("Error!");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
    }
    
    public ArrayList<String> getEntityList() throws IOException, JSONException, CiviCRMException {
    	ArrayList<String> entityList = new ArrayList<String>();
    	URL url = getUrl("Entity", "get", "");
    	HttpURLConnection conn = getHttpConnection(url);
    	String json = getJSonString(conn);

    	assert isValidJSONObject(json) : "String is not a valid JSON.";

    	JSONObject response = new JSONObject(json);

    	// Aqui debo chequear si hay un error de acceso
    	if (response.get("is_error").toString().equals("1")) {
    		throw new CiviCRMException("CiviCRM API Error: " + response.get("error_message").toString());
    	} else
    	if (isValidJSONArray(response.get("values").toString())) {
    		JSONArray values = response.getJSONArray("values");

    		for (int i = 0; i <= values.length() - 1; i++) {
    			entityList.add(values.get(i).toString());
    		}
    	} else {
    		System.out.println("Error!");
    	}
        return entityList;
    }

    public List<JSONString> getNextValues() throws IOException, JSONException, CiviCRMException {
    	ArrayList<JSONString> jsonObjectList = new ArrayList<JSONString>();
    	this.offset = (this.isFirst ? 0 : this.offset + this.limit);
    	this.isFirst = false;
    	String params = (this.options.equals("") ? "" : this.options) + "&options[limit]=" + this.limit
    			+ "&options[offset]=" + this.offset;

    	URL url = getUrl(entity, action, params);
    	HttpURLConnection conn = getHttpConnection(url);
    	String json = getJSonString(conn);


    	assert isValidJSONObject(json) : "String is not a valid JSON.";

    	JSONObject response = new JSONObject(json);

    	// Aqui debo chequear si hay un error de acceso
    	if (response.get("is_error").toString().equals("1")) {
    		throw new CiviCRMException("CiviCRM API Error: " + response.get("error_message").toString());
    	} else
    	if (isValidJSONObject(response.get("values").toString())) {
    		JSONObject values = response.getJSONObject("values");

    		@SuppressWarnings("unchecked")
    		Iterator<String> keysIterator = values.keys();

    		JSONObject jsonObject = null;

    		int index = this.offset;
    		while (keysIterator.hasNext()) {
    			String jsonObjectKey = keysIterator.next();
    			jsonObject = values.getJSONObject(jsonObjectKey);
    			JSONString cjs = new JSONString(jsonObject, index++, jsonObjectKey);
    			jsonObjectList.add(cjs);
    		}
    	} else if (isValidJSONArray(response.get("values").toString())) {
    		JSONArray values = response.getJSONArray("values");

    		for (int i = 0; i <= values.length() - 1; i++) {
    			//System.out.println(values.get(i).toString());
    		}
    	} else {
    		System.out.println("Error!");
    	}
        return jsonObjectList;
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
             * Estos dos metodos hacen lo mismo, depende del gusto el que se use, creo que es mejor
             * usando socket pues es mas simple tener la traza completa para depurar y mantener la
             * conexion abierta hasta finalizar
             */

            msg = tryPostWithUrl(apiUrlString);
            //String result = tryPostWithSocket(apiUrlString, closeConnection);
            // Si aparece la cadena "error_code" se ha producido un error, hay que verificar otros
            // casos con la variable is_error
            error = (msg.indexOf("error_code") >= 0) || (msg.indexOf("error_message") >= 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!error) {
          msg = "";
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

}
