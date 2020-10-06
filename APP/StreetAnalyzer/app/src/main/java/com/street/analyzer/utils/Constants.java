package com.street.analyzer.utils;

import android.hardware.SensorManager;

import com.google.android.gms.location.LocationRequest;

public class Constants {

    public static final String TAG = "[StreetAnalyzer]";

    public static final int ACCELEROMETER_REGISTER_TIME = SensorManager.SENSOR_DELAY_GAME;
    public static final int GRAVITY_REGISTER_TIME = SensorManager.SENSOR_DELAY_NORMAL;

    //TODO: Define default value for Location Update and Limit Position Change
    public static final long LOCATION_UPDATE_INTERVAL = 2000;//30000;
    public static final long LOCATION_UPDATE_FASTEST_INTERVAL = 1000;//20000;
    public static final int LOCATION_LIMIT_POSITION_CHANGE = 3;
    public static final int LOCATION_PRIORITY_HIGH = LocationRequest.PRIORITY_HIGH_ACCURACY;

    public static final String NOTIFICATION_CHANNEL_MSG = "Notification_channel";
    public static final String NOTIFICATION_CHANNEL_ID = "Recording_notification";
    public static final String NOTIFICATION_NAME = "StreetAnalyzer is recording";
    public static final String NOTIFICATION_TEXT = "Thank you for helping. Tap to return to the app";

    public static final String DATA_FILE_NAME = "recordData.data";

    public static final int JOB_UPLOAD_ID = 17037607;

    public static final long TOTAL_DATA_TO_TRANSMIT = 1024 * 1024;

    public static final int JOB_SCHEDULER_PERIOD = 15 * 60 * 1000; //60 * 60 * 1000 //One hour

    public static final int SERVER_RESPONSE_OK = 0;
    public static final String SERVER_LOGIN = "auth";
    public static final String SERVER_SCHEME_HTTPS = "https";
    public static final String SERVER_CREATE_ACCOUNT = "createAccount";
    public static final String SERVER_HOST = "street-analyzer.herokuapp.com";
    public static final String SERVER_VERIFY_ACCOUNT = "verifyAccount";
    public static final String SERVER_LOG_OUT = "deauth";
    //TODO: Verify if the path is right.
    public static final String SERVER_UPLOAD_LOG = "logTrip";
    public static final String SERVER_QUALITY_OVERLAY = "qualityOverlay";

    public static final int SERVER_ERRO_INVALID_CREDENTIALS = 8;

    public static final int MIN_NAME_LENGTH = 4;
    public static final int MIN_PASSWORD_LENGTH = 4;

    public static final String EXTRA_CREATE_ACCOUNT = "CreateAccount";

    public static final int IMAGE_GALLERY_REQUEST = 20;

    public static final String REMEMBER_ME_STATUS_KEY = "RememberMeStatusKey";
    public static final String REMEMBER_ME_EMAIL_KEY = "RememberMeEmailKey";
    public static final String REMEMBER_ME_PASSWORD_KEY = "RememberMePasswordKey";

    //MapsActivity
    public static final String TOAST_BACK_EXIT = "Please click BACK again to exit";
    public static final String TOAST_YOU = "You";
    public static final String TOAST_CURRENT_LOCATION = "Going to current location";

    //RecordService
    public static final String TOAST_NULL_SERVICE = "Null service!";

    //CreateAccountActivity
    public static final String TOAST_NETWORK_NOT_DETECTED = "Network not detected\nMake sure you are connected to the internet";
    public static final String TOAST_UNABLE_OPEN_IMAGE = "Unable to open image";
    public static final String MSG_INVALID_EMAIL = "Invalid email!\n";
    public static final String MSG_INVALID_NAME = "Invalid name, your name must have at least " + Constants.MIN_NAME_LENGTH + " characters!\n";
    public static final String MSG_INVALID_PASSWORD = "Invalid password, your password must have at least " + Constants.MIN_PASSWORD_LENGTH + " characters!\n";
    public static final String MSG_PASSWORD_MISMATCH =  "Passwords don't match!";
    public static final String ALERT_INVALID_INPUT = "Input not valid!";

    //LoginActivity
    public static final String TOAST_TURN_ON_LOCATION = "Please, turn on device location";
    public static final String TOAST_LOGIN_FAILURE = "Sorry, we can't login!\nFailed to communicate with server";
    public static final String ALERT_VERIFY_EMAIL = "Only one more step!\nPlease check your mail box to verify your email address!";

    public static final String USER_NAME_KEY = "UserName";
    public static final String USER_TOKEN_KEY = "UserToken";
    public static final String USER_EMAIL_KEY = "UserEmail";
    public static final String USER_PICTURE_KEY = "UserProfilePicture";
    public static final String USER_DATA = "UserData";

    public static final int MAX_SEND_DATA = 2; //The minimum possible value is 2
}
