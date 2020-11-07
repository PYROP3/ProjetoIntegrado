package com.street.analyzer.serverCommunication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Handler;

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

    private Bitmap request(Context context, final String stringUrl){
        final Bitmap[] result = {null};

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(stringUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(60000 /* milliseconds */);
                    conn.setConnectTimeout(65000 /* milliseconds */);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.connect();

                    int response = conn.getResponseCode();
                    SLog.d(TAG, "The response is: " + response);

                    InputStream is = conn.getInputStream();
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(is);
                    Bitmap bmpImage = BitmapFactory.decodeStream(bufferedInputStream);
                    result[0] = bmpImage;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        SLog.d(TAG, "Bitmap received status: " + (result[0] == null ? "ERROR" : "SUCCESS"));

        return result[0];
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

        int index = 1;
        while(index < recordedValues.getSize()-1) {

            jsonObject = jsonParser.createLogToSend(recordedValues, name, index, index + 1);
            index++;

            if(index == recordedValues.getSize()-1)
                isLast = true;

            RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)//TODO: move to constants
                    .post(requestBody)
                    .build();
            SLog.d(TAG, "Enqueuing new retrofit callback [LogTrip]");

            if (!isLast) {
                try {
                    //TODO: Handle the response
                    SLog.d(TAG, "Sending registered data [" + index + "]");
                    Response response = okHttpClient.newCall(request).execute();

                    if(response.isSuccessful()){
                        SLog.d(TAG, "Response: SUCCESS");
                    }else{
                        SLog.d(TAG, "Response: ERROR");
                        SLog.d(TAG, "Response body: " + response.body().string());

                    }
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

    public boolean authenticateAccount(Context context, String token){
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

        try {
            Response r = okHttpClient.newCall(request).execute();
            if(r.isSuccessful()){
                SLog.d(TAG, "ResponseSuccessful!");
                return true;
            }else{
                SLog.d(TAG, "ResponseFailure: " + r.body().string());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
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

//TODO: Delete this comment later, not now because it can still be useful (what I'm doing with my life)
//            if (recordedValues.getSize() > Constants.MAX_SEND_DATA){
//                SLog.d(TAG, "Sending " + Constants.MAX_SEND_DATA + " LOGS");
//                jsonObject = jsonParser.createLogToSend(recordedValues, name, Constants.MAX_SEND_DATA);
//                recordedValues.splitData(Constants.MAX_SEND_DATA);
//            }else{
//                isLast = true;
//                jsonObject = jsonParser.createLogToSend(recordedValues, name, recordedValues.getSize());
//                SLog.d(TAG, "Last Sending " + (recordedValues.getSize()) + " LOGS");
//            }
//            SLog.d(TAG, "Sending: " + jsonObject.toString());


//        try {
//            DownloadImageTask downloadImageTask = new DownloadImageTask();
//            downloadImageTask.execute(httpUrl.toString())
//        } catch (ExecutionException e) {
//            SLog.d(TAG, "Deu ruim ExecutioN");
//        } catch (InterruptedException e) {
//            SLog.d(TAG, "Deu ruim Interrupted");
//        }

//        try {
//            url = new URL(httpUrl.toString());
//            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
//            SLog.d(TAG, "Returning bitmap");
//            return bmp;
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//            return null;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//        SLog.d(TAG, "Enqueuing new retrofit callback [RequestQualityOverlay]");
//
//        okHttpClient.newCall(request).enqueue(callback);