package com.street.analyzer.serverCommunication;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.street.analyzer.utils.Constants;

public class CustomOkHttpClient {

    final String TAG = Constants.TAG;

    public boolean requestJsonTest(Context context, Callback callback){

        if(!NetworkStatusManager.isNetworkAvailable(context)){
            Log.d(TAG, "Network not available");
            return false;
        }

        OkHttpClient okHttpClient = new OkHttpClient();

        Request request = new Request.Builder()
                .url(Constants.SERVER_URL)
                .build();

        Log.d(TAG, "Enqueuing new retrofit callback");

        okHttpClient.newCall(request).enqueue(callback);
        return true;
    }
}