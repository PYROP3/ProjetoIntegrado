package com.street.analyzer.serverCommunication;

import android.app.slice.Slice;
import android.content.Context;
import android.content.SharedPreferences;

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
                                      Values recordedValues, String name, String token){

        boolean isLast = false;

        if(!isNetworkAvailable(context))
            return false;

        OkHttpClient okHttpClient = new OkHttpClient();
        JsonParser jsonParser = new JsonParser();
        JSONObject jsonObject = null;

        HttpUrl url = new HttpUrl.Builder()
                .scheme(Constants.SERVER_SCHEME_HTTPS)
                .host(Constants.SERVER_HOST)
                .addPathSegment(Constants.SERVER_UPLOAD_LOG)
                .build();

        SLog.d(TAG, "Sending request to: " + url.toString());

        int size = recordedValues.getSize();


        while(recordedValues.getSize() > 0) {

            if (recordedValues.getSize() > 10){
                SLog.d(TAG, "Sending 10 LOGS");
                jsonObject = jsonParser.createLogToSend(recordedValues, name, 10);
                recordedValues.splitData(10);
            }else{
                isLast = true;
                jsonObject = jsonParser.createLogToSend(recordedValues, name, recordedValues.getSize());
                SLog.d(TAG, "Last Sending " + (recordedValues.getSize()) + " LOGS");
            }

            RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)//TODO: move to constants
                    .post(requestBody)
                    .build();
            SLog.d(TAG, "TOKEN " + token);
            SLog.d(TAG, "Enqueuing new retrofit callback [LogTrip]");

            if (!isLast) {
                try {
                    //TODO: Handle the response
                    Response response = okHttpClient.newCall(request).execute();
                    SLog.d(TAG, "Response: " + (response.isSuccessful() ? "SUCCESS" : "FAIL"));
                } catch (IOException e) {
                    //TODO: TODO
                    SLog.d(TAG, "Error trying to send log");
                }
            } else {
                okHttpClient.newCall(request).enqueue(callback);
                break;
            }
        }

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

    public synchronized boolean sendEndSession(Context context, String token){
        if(!isNetworkAvailable(context))
            return false;

        SLog.d(TAG, "Preparing end session request");

        OkHttpClient okHttpClient = new OkHttpClient();

        HttpUrl url = new HttpUrl.Builder()
                .scheme(Constants.SERVER_SCHEME_HTTPS)
                .host(Constants.SERVER_HOST)
                .addPathSegment(Constants.SERVER_LOG_OUT)
                .build();

        SLog.d(TAG, "Sending request to: " + url.toString());

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + token)
                .url(url)
                .build();

        SLog.d(TAG, "Enqueuing new retrofit callback [SendEndSession]");

        okHttpClient.newCall(request).enqueue(this);

        SLog.d(TAG, "Logout request sent");

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
        }else{
            SLog.d(TAG, "Response is not successfully");
        }
        SLog.d(TAG, "Return: " + response.body().string());
    }

    @Override
    public void onFailure(Request request, IOException e) {
        SLog.d(TAG, "Fail onFailure");
    }
}