package com.street.analyzer.record;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.street.analyzer.R;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.location.MapsActivity;

import java.util.ArrayList;
import java.util.List;

public class RecordService extends Service implements SensorEventListener {

    private final String TAG = Constants.TAG;

    private float mAlpha = (float)0.8;
    private float mGravity[] = new float[]{(float)9.81, (float)9.81, (float)9.81};

    private StorageManager mStorageManager;

    private SensorManager mSensorManager;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Deprecated
    public void onCreate() {
        super.onCreate();
        mStorageManager = new StorageManager(this);

        Log.d(TAG, "Registering listeners and starting service");

       registerSensorListener();
       registerLocaleListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        createNotificationChannel();

        Intent notificationIntent = new Intent (this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle(Constants.NOTIFICATION_NAME)
                .setContentText(Constants.NOTIFICATION_TEXT)
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        return START_NOT_STICKY;
    }

    private void registerSensorListener(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if(mSensorManager == null){
            Toast.makeText(this, "Null service!", Toast.LENGTH_SHORT).show();
            return;
        }

        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, sensor, Constants.ACCELEROMETER_REGISTER_TIME);
        mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mSensorManager.registerListener(this, sensor, Constants.GRAVITY_REGISTER_TIME);
    }

    private void registerLocaleListener(){
        final LocationRequest locationRequest = LocationRequest.create()
                .setInterval(Constants.LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(Constants.LOCATION_UPDATE_FASTEST_INTERVAL)
                .setPriority(Constants.LOCATION_PRIORITY_HIGH);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                if(locationResult == null) return;
                Location location = locationResult.getLastLocation();
                mStorageManager.registerPositionChange(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "New location registered");
            }
        };
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                mGravity[0] = mAlpha * mGravity[0] + (1 - mAlpha) * event.values[0];
                mGravity[1] = mAlpha * mGravity[1] + (1 - mAlpha) * event.values[1];
                mGravity[2] = mAlpha * mGravity[2] + (1 - mAlpha) * event.values[2];

                event.values[0] = event.values[0]- mGravity[0];
                event.values[1] = event.values[1]- mGravity[1];
                event.values[2] = event.values[2]- mGravity[2];

                mStorageManager.registerAccelerometerData(event.values);
                break;
            case Sensor.TYPE_GRAVITY:
                mGravity[0] = event.values[0];
                mGravity[1] = event.values[1];
                mGravity[2] = event.values[2];
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    Constants.NOTIFICATION_CHANNEL_MSG,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy: Unregistering listeners");
        mSensorManager.unregisterListener(this);
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }
}