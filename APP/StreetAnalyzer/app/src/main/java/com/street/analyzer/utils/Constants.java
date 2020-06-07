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
    public static final int LOCATION_LIMIT_POSITION_CHANGE = 3;
    public static final int LOCATION_PRIORITY_HIGH = LocationRequest.PRIORITY_HIGH_ACCURACY;

    public static final String NOTIFICATION_CHANNEL_MSG = "Notification_channel";
    public static final String NOTIFICATION_CHANNEL_ID = "Recording_notification";
    public static final String NOTIFICATION_NAME = "We are recording";
    public static final String NOTIFICATION_TEXT = "Thank you bla bla bla";

    public static final String DATA_FILE_NAME = "recordData.data";

    public static final int JOB_UPLOAD_ID = 17037607;

    public static final long TOTAL_DATA_TO_TRANSMIT = 1024 * 1024;

    public static final int JOB_SCHEDULER_PERIOD = 15 * 60 * 1000; //60 * 60 * 1000 //One hour

    public static final String SERVER_HOST = "street-analyzer.herokuapp.com";
    public static final String SERVER_SCHEME_HTTPS = "https";
    public static final String SERVER_CREATE_ACCOUNT = "createAccount";

    public static final int SERVER_RESPONSE_OK = 0;

    public static final int MIN_NAME_LENGTH = 4;
    public static final int MIN_PASSWORD_LENGTH = 4;

    public static final String EXTRA_CREATE_ACCOUNT = "CreateAccount";
}
