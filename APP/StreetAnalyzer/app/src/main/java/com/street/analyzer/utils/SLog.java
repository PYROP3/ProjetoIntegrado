package com.street.analyzer.utils;

import android.util.Log;

public class SLog {
    public static void d(String TAG, String message){
        Log.d(Constants.TAG, TAG + ": " +  message);
    }
}
