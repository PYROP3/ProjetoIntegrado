package com.street.analyzer.serverCommunication;

import android.content.Context;
import android.net.ConnectivityManager;

class NetworkStatusManager /*extends ConnectivityManager.NetworkCallback*/ {

    static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null)
            return false;

            return connectivityManager.getActiveNetworkInfo() != null
                    && connectivityManager.getActiveNetworkInfo().isConnected();
    }

}
