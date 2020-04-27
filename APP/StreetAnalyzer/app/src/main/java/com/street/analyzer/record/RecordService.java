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
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.street.analyzer.R;
import com.street.analyzer.Utils.Constants;
import com.street.analyzer.location.MapsActivity;

import java.util.ArrayList;

public class RecordService extends Service implements SensorEventListener {

    private float mAlpha = (float)0.8;
    private float mGravity[] = new float[]{(float)9.81, (float)9.81, (float)9.81};

    private ArrayList<Values> data = new ArrayList<>();

    private Values mValues;
    private LocationCallback mLocationCallback;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Deprecated
    public void onCreate() {
        super.onCreate();
        mValues = new Values();

       registerSensor();
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

    private void registerSensor(){
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if(sensorManager == null){
            Toast.makeText(this, "Null service!", Toast.LENGTH_SHORT).show();
            return;
        }

        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, Constants.ACCELEROMETER_REGISTER_TIME);
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(this, sensor, Constants.GRAVITY_REGISTER_TIME);
    }

    private void registerLocaleListener(){
        final LocationRequest locationRequest = LocationRequest.create()
                .setInterval(Constants.LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(Constants.LOCATION_UPDATE_FASTEST_INTERVAL)
                .setPriority(Constants.LOCATION_PRIORITY_HIGH);

        FusedLocationProviderClient fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                if(locationResult == null)
                    return;
                mValues.registerPositionChange(locationResult.getLastLocation());
                showMessage();
            }
        };;

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
    }

    private void showMessage(){
        Toast.makeText(this, "LOCATION CHANGE", Toast.LENGTH_SHORT).show();
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

                mValues.registerAccelerometerData(event.values);

                data.add(mValues);

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
}
