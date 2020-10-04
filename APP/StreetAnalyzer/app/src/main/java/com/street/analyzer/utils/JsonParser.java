package com.street.analyzer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.street.analyzer.record.Values;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class JsonParser {

    private static final String TAG = "JsonParser";

    public JSONObject createAccountJson(String name, String email, String password){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("email", email);
            jsonObject.put("name", name);
            jsonObject.put("password", password);
            SLog.d(TAG, "JSON: " + jsonObject.toString());
        } catch (JSONException e) {
            SLog.d(TAG, "Error trying to create JSON");
            e.printStackTrace();
        }
        SLog.d(TAG, "JSON created");
        return jsonObject;
    }

    public JSONObject loginJson(String email, String password){
        JSONObject jsonObject = new JSONObject();

        try{
            jsonObject.put("email", email);
            jsonObject.put("password", password);
        }catch (JSONException e){
            SLog.d(TAG, "Error trying to create JSON");
            e.printStackTrace();
        }
        SLog.d(TAG, "JSON created");
        return jsonObject;
    }

    public JSONObject createLogToSend(Values values, String userName, int start, int end){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("usuario", userName);

            JSONArray jsonArray = new JSONArray();
            JSONArray jsonArrayAux = new JSONArray();
            JSONArray aux = new JSONArray();

            aux.put(values.getLatitude().get(start));
            aux.put(values.getLongitude().get(start));

            jsonArray.put(aux);
            aux = new JSONArray();

            aux.put(values.getLatitude().get(end));
            aux.put(values.getLongitude().get(end));
            jsonArray.put(aux);

            SLog.d(TAG, "Locations: " + jsonArray.toString());
            jsonObject.put("pontos", jsonArray);

            jsonArray = new JSONArray();
            aux = new JSONArray();

            int index = 0;
            for(int i = 0; i < start; i++)
                index = values.getCounters().get(i);

            for(int i = 0; i < index; i++){
                    aux.put(values.getXValue().get(index));
                    aux.put(values.getYValue().get(index));
                    aux.put(values.getZValue().get(index));

                    jsonArray.put(aux);
                    aux = new JSONArray();
            }

            jsonArrayAux.put(jsonArray);


            SLog.d(TAG, "Data: " + jsonArrayAux.toString());
            jsonObject.put("dados", jsonArrayAux);

        }catch (JSONException e){
            SLog.d(TAG, "Error trying to create JSON");
            e.printStackTrace();
        }

        SLog.d(TAG, "Log JSON created");
        SLog.d(TAG, jsonObject.toString());

        return jsonObject;
    }

    public HashMap<String, String> parseLoginResponse(String response){
        HashMap<String, String> s = new HashMap<>();
        try{
            JSONObject jsonObject = new JSONObject(response);

            String name = jsonObject.getString("name");
            String token = jsonObject.getString("authToken");
            String email = jsonObject.getString("email");

            s.put(Constants.USER_NAME_KEY, name);
            s.put(Constants.USER_EMAIL_KEY, email);
            s.put(Constants.USER_TOKEN_KEY, token);
            //s.put(Constants.USER_PICTURE_KEY, jsonObject.getString("email"));

        }catch(JSONException e){
            e.printStackTrace();
        }

        return s;
    }

    public static boolean isResponseSuccessful(String jsonResponse){
       try {
           SLog.d(TAG, jsonResponse);
           JSONObject jsonObject = new JSONObject(jsonResponse);
           if(jsonObject.getInt("Code") == Constants.SERVER_RESPONSE_OK){
               return true;
           }
       } catch (JSONException e) {
           e.printStackTrace();
       }
        return false;
    }

    public static String getQueryToken(String dataString) {
        Uri uri = Uri.parse(dataString);
        String args = uri.getQueryParameter("token");
        SLog.d(TAG, "User token: " + args);
        return args;
    }
}

//TODO: Delete this comment later, not now because it can still be useful (what I'm doing with my life)
//            for(int i = 0; i < end; i++){
//                aux.put(values.getLatitude().get(i));
//                aux.put(values.getLongitude().get(i));
//                jsonArray.put(aux);
//                aux = new JSONArray();
//            }
//            for(int i = 0; i < values.getSize(); i++){
//                aux.put(values.getLatitude().get(i));
//                aux.put(values.getLongitude().get(i));
//                jsonArray.put(aux);
//                aux = new JSONArray();
//            }
//            for(int i = 0; i < end; i++){
//                if(i >= values.getCounters().size())
//                    break;
//                for(int j = 0; j < values.getCounters().get(i); j++){
//                    if(index >= values.getXValue().size())
//                        break;
//                    aux.put(values.getXValue().get(index));
//                    aux.put(values.getYValue().get(index));
//                    aux.put(values.getZValue().get(index));
//                    jsonArray.put(aux);
//                    aux = new JSONArray();
//                    index += 1;
//                }
//                jsonArrayAux.put(jsonArray);
//            }
//            int indice = 0;
//            for(Integer x : values.getCounters()){
//                for(int i = 0; i < x; i++){
//                    aux.put(values.getXValue().get(indice));
//                    aux.put(values.getYValue().get(indice));
//                    aux.put(values.getZValue().get(indice));
//                    jsonArray.put(aux);
//                    aux = new JSONArray();
//                    indice++;
//                }
//            }
