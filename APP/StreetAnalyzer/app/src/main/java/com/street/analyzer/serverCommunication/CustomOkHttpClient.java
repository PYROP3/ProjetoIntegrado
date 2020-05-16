package com.street.analyzer.serverCommunication;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.street.analyzer.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomOkHttpClient {

    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final String TAG = Constants.TAG;

    public boolean sendCreateAccountRequest(Context context, Callback callback){
        if(!isNetworkAvailable(context))
            return false;

        OkHttpClient okHttpClient = new OkHttpClient();

        HttpUrl url = new HttpUrl.Builder()
                .scheme(Constants.SEND_SCHEME_HTTPS)
                .host(Constants.SEND_HOST)
                .addPathSegment(Constants.SEND_API_PATH)
                .addPathSegment(Constants.SEND_REGISTER_PATH)
                .build();

        Log.d(TAG, "Sending request to: " + url.toString());

        JSONObject jsonObject = JsonParser.createAccountJson("eve.holt@reqres.in", "pistol");
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Log.d(TAG, "Enqueuing new retrofit callback");

        okHttpClient.newCall(request).enqueue(callback);

        return true;
    }




    public boolean requestJsonTest(Context context, Callback callback){
        if(!isNetworkAvailable(context))
            return false;

        OkHttpClient okHttpClient = new OkHttpClient();

        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("reqres.in")
                .addPathSegment("api")
                .addPathSegment("users")
                .build();

        Log.d(TAG, "Sending request to: " + url.toString());

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("name","morpheus");
            jsonObject.put("job", "leader");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "JSON: " + jsonObject.toString());

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Log.d(TAG, "Enqueuing new retrofit callback");

        okHttpClient.newCall(request).enqueue(callback);
        return true;
    }

    private boolean isNetworkAvailable(Context context){
        if (NetworkStatusManager.isNetworkAvailable(context)){
            return true;
        }
        Log.d(TAG, "Network not available");
        return false;
    }
}