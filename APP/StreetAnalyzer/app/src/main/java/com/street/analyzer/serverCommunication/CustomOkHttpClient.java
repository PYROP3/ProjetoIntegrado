package com.street.analyzer.serverCommunication;

import android.app.slice.Slice;
import android.content.Context;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.street.analyzer.record.Values;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.JsonParser;
import com.street.analyzer.utils.SLog;

import org.json.JSONObject;

import java.io.IOException;

public class CustomOkHttpClient implements Callback{

    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final String TAG = getClass().getSimpleName();

    public boolean sendCreateAccountRequest(Context context, Callback callback,
                        String email, String name, String password){

        if(!isNetworkAvailable(context))
            return false;

        OkHttpClient okHttpClient = new OkHttpClient();

        HttpUrl url = new HttpUrl.Builder()
                .scheme(Constants.SERVER_SCHEME_HTTPS)
                .host(Constants.SERVER_HOST)
                .addPathSegment(Constants.SERVER_CREATE_ACCOUNT)
                .build();

        SLog.d(TAG, "Sending request to: " + url.toString());

        JsonParser jsonParser = new JsonParser();
        JSONObject jsonObject = jsonParser.createAccountJson(name, email, password);
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        SLog.d(TAG, "Enqueuing new retrofit callback [CreateAccount]");

        okHttpClient.newCall(request).enqueue(callback);

        return true;
    }

    public boolean sendLoginRequest(Context context, Callback callback, String email, String password){
        if(!isNetworkAvailable(context))
            return false;

        OkHttpClient okHttpClient = new OkHttpClient();

        HttpUrl url = new HttpUrl.Builder()
                .scheme(Constants.SERVER_SCHEME_HTTPS)
                .host(Constants.SERVER_HOST)
                .addPathSegment(Constants.SERVER_LOGIN)
                .build();


        SLog.d(TAG, "Sending request to: " + url.toString());

        JsonParser jsonParser = new JsonParser();
        JSONObject jsonObject = jsonParser.loginJson(email, password);
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        SLog.d(TAG, "Enqueuing new retrofit callback [CreateAccount]");

        okHttpClient.newCall(request).enqueue(callback);

        return true;
    }

    public boolean sendRegisteredData(Context context, Callback callback,
                                      Values recordedValues){

        if(!isNetworkAvailable(context))
            return false;

        OkHttpClient okHttpClient = new OkHttpClient();

        HttpUrl url = new HttpUrl.Builder()
                .scheme(Constants.SERVER_SCHEME_HTTPS)
                .host(Constants.SERVER_HOST)
                .addPathSegment(Constants.SERVER_UPLOAD_LOG)
                .build();

        SLog.d(TAG, "Sending request to: " + url.toString());

        JsonParser jsonParser = new JsonParser();
        JSONObject jsonObject = jsonParser.createLogToSend(recordedValues);

        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        SLog.d(TAG, "Enqueuing new retrofit callback [LogTrip]");

        okHttpClient.newCall(request).enqueue(callback);

        return true;
    }

    public boolean authenticateAccount(Context context, Callback callback, String token){
        if(!isNetworkAvailable(context))
            return false;

        OkHttpClient okHttpClient = new OkHttpClient();

        HttpUrl url = new HttpUrl.Builder()
                .scheme(Constants.SERVER_SCHEME_HTTPS)
                .host(Constants.SERVER_HOST)
                .addPathSegment(Constants.SERVER_VERIFY_ACCOUNT)
                .addQueryParameter("token", token)
                .build();

        SLog.d(TAG, "Sending request to: " + url.toString());

        Request request = new Request.Builder()
                .url(url)
                .build();

        SLog.d(TAG, "Enqueuing new retrofit callback [VerifyAccount]");

        okHttpClient.newCall(request).enqueue((callback == null) ? this : callback);

        return true;
    }

    private boolean isNetworkAvailable(Context context){
        if (NetworkStatusManager.isNetworkAvailable(context)){
            return true;
        }
        SLog.d(TAG, "Network not available");
        return false;
    }

    @Override
    public void onResponse(Response response) throws IOException {
        if(response.isSuccessful()){
            SLog.d(TAG, "Response successfully");
        }
        else{
            SLog.d(TAG, "Response is not successfully");
        }
    }

    @Override
    public void onFailure(Request request, IOException e) {
        SLog.d(TAG, "Fail onFailure");
    }
}