package com.street.analyzer.utils;

import android.hardware.SensorManager;

import com.google.android.gms.location.LocationRequest;

public class Constants {

    public static final String TAG = "[StreetAnalyzer]";

    public static final int ACCELEROMETER_REGISTER_TIME = SensorManager.SENSOR_DELAY_GAME;
    public static final int GRAVITY_REGISTER_TIME = SensorManager.SENSOR_DELAY_NORMAL;

    //TODO: Define default value for Location Update and Limit Position Change
    public static final long LOCATION_UPDATE_INTERVAL = 1000;//30000;
    public static final long LOCATION_UPDATE_FASTEST_INTERVAL = 1000;//20000;
    public static final int LOCATION_LIMIT_POSITION_CHANGE = 5;
    public static final int LOCATION_PRIORITY_HIGH = LocationRequest.PRIORITY_HIGH_ACCURACY;

    public static final String NOTIFICATION_CHANNEL_MSG = "Notification_channel";
    public static final String NOTIFICATION_CHANNEL_ID = "Recording_notification";
    public static final String NOTIFICATION_NAME = "We are recording";
    public static final String NOTIFICATION_TEXT = "Thank you bla bla bla";

    public static final String DATA_FILE_NAME = "recordData.data";

    public static final int JOB_UPLOAD_ID = 17037607;

    public static final long TOTAL_DATA_TO_TRANSMIT = 1024 * 1024;

    public static final int JOB_SCHEDULER_PERIOD = 15 * 60 * 1000; //60 * 60 * 1000 //One hour

    //Only for test
    public static final String MY_ADDRESS = "http//10.0.0.144:8080";
    public static final String SEND_SCHEME_HTTPS = "https";
    public static final String SEND_HOST = "reqres.in";
    public static final String SEND_API_PATH = "api";
    public static final String SEND_USERS_PATH = "users";
    public static final String SEND_REGISTER_PATH = "register";

}
