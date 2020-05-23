package com.street.analyzer.serverCommunication;

import com.street.analyzer.utils.SLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

class JsonParser {

    private static final String TAG = "JsonParser";

    static JSONObject createAccountJson(String name, String email){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("email",name);
            jsonObject.put("password", email);
            SLog.d(TAG, "JSON: " + jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }
}
