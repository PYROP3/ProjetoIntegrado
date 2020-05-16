package com.street.analyzer.serverCommunication;

import android.util.Log;

import com.street.analyzer.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

class JsonParser {

    private static final String TAG = Constants.TAG;

    static JSONObject createAccountJson(String name, String email){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("email",name);
            jsonObject.put("password", email);
            Log.d(TAG, "JSON: " + jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }
}
