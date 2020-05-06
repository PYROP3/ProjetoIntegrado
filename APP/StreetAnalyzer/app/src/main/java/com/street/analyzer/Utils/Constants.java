package com.street.analyzer.Utils;

import android.hardware.SensorManager;

import com.google.android.gms.location.LocationRequest;

public class Constants {

    public static final int ACCELEROMETER_REGISTER_TIME = SensorManager.SENSOR_DELAY_GAME;
    public static final int GRAVITY_REGISTER_TIME = SensorManager.SENSOR_DELAY_NORMAL;

    public static final long LOCATION_UPDATE_INTERVAL = 30000;
    public static final long LOCATION_UPDATE_FASTEST_INTERVAL = 20000;
    public static final int LOCATION_PRIORITY_HIGH = LocationRequest.PRIORITY_HIGH_ACCURACY;

    public static final String NOTIFICATION_CHANNEL_MSG = "Notification_channel";
    public static final String NOTIFICATION_CHANNEL_ID = "Recording_notification";
    public static final String NOTIFICATION_NAME = "We are recording";
    public static final String NOTIFICATION_TEXT = "Thank you bla bla bla";
}
