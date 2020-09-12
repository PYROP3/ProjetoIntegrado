package com.street.analyzer.utils;

import android.content.Context;
import android.content.SharedPreferences;

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

    public JSONObject createLogToSend(Values values, String userName){
        JSONObject jsonObject = new JSONObject();

        try {
            SLog.d(TAG, "USER: " + userName);
            jsonObject.put("usuario", userName);

            JSONArray jsonArray = new JSONArray();
            JSONArray aux = new JSONArray();
            for(int i = 0; i < values.getLatitude().size(); i++){
                aux.put(values.getLatitude().get(i));
                aux.put(values.getLongitude().get(i));
                jsonArray.put(aux);
                aux = new JSONArray();
            }

            jsonObject.put("pontos", jsonArray);

            jsonArray = new JSONArray();
            aux = new JSONArray();

            int indice = 0;
            for(Integer x : values.getCounters()){
                for(int i = 0; i < x; i++){
                    aux.put(values.getXValue().get(indice));
                    aux.put(values.getYValue().get(indice));
                    aux.put(values.getZValue().get(indice));

                    jsonArray.put(aux);
                    aux = new JSONArray();
                    indice++;
                }
            }

            jsonObject.put("dados", jsonArray);

        }catch (JSONException e){
            SLog.d(TAG, "Error trying to create JSON");
            e.printStackTrace();
        }

        SLog.d(TAG, "Log JSON created");
        SLog.d(TAG, jsonObject.toString());

        return jsonObject;
    }

    public HashMap<String, String> parseLoginResponse(String response){
        SLog.d(TAG, "Response: " + response);
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
}
