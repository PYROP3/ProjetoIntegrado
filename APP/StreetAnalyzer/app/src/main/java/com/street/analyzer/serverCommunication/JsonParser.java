package com.street.analyzer.serverCommunication;

import com.street.analyzer.record.Values;
import com.street.analyzer.utils.SLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class JsonParser {

    private static final String TAG = "JsonParser";

    JSONObject createAccountJson(String name, String email){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("email",name);
            jsonObject.put("password", email);
            SLog.d(TAG, "JSON: " + jsonObject.toString());
        } catch (JSONException e) {
            SLog.d(TAG, "Error trying to create JSON");
            e.printStackTrace();
        }
        SLog.d(TAG, "JSON created");
        return jsonObject;
    }

    JSONObject createLogToSend(Values values){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("usuario", "casalbé");

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
            for(int i = 0; i < values.getXValue().size(); i++){
                aux.put(values.getXValue().get(i));
                aux.put(values.getYValue().get(i));
                aux.put(values.getZValue().get(i));
                jsonArray.put(aux);
                aux = new JSONArray();
            }

            jsonObject.put("pontos", jsonArray);



        }catch (JSONException e){
            SLog.d(TAG, "Error trying to create JSON");
            e.printStackTrace();
        }

        SLog.d(TAG, "Log JSON created");
        SLog.d(TAG, jsonObject.toString());

        return null;
    }


}
