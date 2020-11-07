package com.street.analyzer.serverCommunication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.squareup.okhttp.HttpUrl;
import com.street.analyzer.location.MapsActivity;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.net.URL;
import java.net.URLConnection;

public class OverlayRequest implements Runnable {

    private static final String TAG = OverlayRequest.class.getSimpleName();

    private double minLat, maxLat, minLong, maxLong;
    private volatile Bitmap bitmap;
    private MapsActivity mapsActivity;

    public OverlayRequest(double minLat, double maxLat, double minLong, double maxLong,
                          MapsActivity mapsActivity){
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLong = minLong;
        this.maxLong = maxLong;
        this.mapsActivity = mapsActivity;
    }

    @Override
    public void run() {
        final HttpUrl httpUrl = new HttpUrl.Builder()
            .scheme(Constants.SERVER_SCHEME_HTTPS)
            .host(Constants.SERVER_HOST)
            .addPathSegment(Constants.SERVER_QUALITY_OVERLAY)
            .addQueryParameter("minLatitude", "" + minLat)
            .addQueryParameter("maxLatitude", "" + maxLat)
            .addQueryParameter("minLongitude", "" + minLong)
            .addQueryParameter("maxLongitude", "" + maxLong)
            .build();

        SLog.d(TAG, "Sending request to: " + httpUrl.toString());

        try {
            URLConnection conn = new URL(httpUrl.toString()).openConnection();
            conn.setUseCaches(true);
            bitmap = BitmapFactory.decodeStream(conn.getInputStream());
            SLog.d(TAG, "Overlay request return: " + bitmap);
            mapsActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapsActivity.setNewGroundOverlay(bitmap);
                }
            });
        }catch(Exception e){
            SLog.d(TAG, "ERROR: " + e);
        }
    }
}
