package com.street.analyzer.serverCommunication;

import android.app.job.JobInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.UrlQuerySanitizer;

import com.street.analyzer.record.SaveState;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.util.Calendar;
import java.util.Set;

public class NetworkStatusManager {

    private static final String TAG = "NetworkStatusManager";

    private static final String PREFERENCES_NAME = "dataTransmittedValue";
    private static final String TOTAL_DATA_TRANSMITTED = "totalDataTransmitted";

    static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager == null)
                return false;

            return connectivityManager.getActiveNetworkInfo() != null
                    && connectivityManager.getActiveNetworkInfo().isConnected();
        }catch(Exception e){
            SLog.d(TAG, "Android returned null connectivity manager");
            return true;
        }
    }

    public static int networkAllowedToSend(Context context){
        long totalData = getCurrentDataToTransmit(context) + getTotalDataTransmitted(context);

        if(Constants.TOTAL_DATA_TO_TRANSMIT < totalData){
            SLog.d(TAG, "Is allowed to send only in unmetered connection");
            return JobInfo.NETWORK_TYPE_UNMETERED;
        }

        SLog.d(TAG, "Is allowed to send in any connection");
        return JobInfo.NETWORK_TYPE_ANY;
    }

    private static long getCurrentDataToTransmit(Context context){
        SaveState saveState = SaveState.getInstance();
        long currentDataSize = saveState.getCurrentDataSize();
        SLog.d(TAG, "Current data to transmit: " + currentDataSize);
        return currentDataSize;
    }

    private static int getTotalDataTransmitted(Context context){
        needToResetCounter(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        int totalData = sharedPreferences.getInt(TOTAL_DATA_TRANSMITTED, 0);
        SLog.d(TAG, "Total data transmitted: " + totalData);
        return totalData;
    }

    private static void needToResetCounter(Context context){
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        SLog.d(TAG, "Reset calendar: " + day);
        if(day == 1)
            resetTotalDataTransmitted(context);
    }

    private static void resetTotalDataTransmitted(Context context){
        SharedPreferences.Editor editor =
                (SharedPreferences.Editor) context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        editor.putInt(TOTAL_DATA_TRANSMITTED, 0);
        editor.apply();
    }
}
