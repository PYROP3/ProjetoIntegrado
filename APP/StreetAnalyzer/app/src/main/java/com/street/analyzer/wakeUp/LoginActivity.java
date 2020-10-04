package com.street.analyzer.wakeUp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.street.analyzer.location.MapsActivity;
import com.street.analyzer.R;
import com.street.analyzer.serverCommunication.CustomOkHttpClient;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.JsonParser;
import com.street.analyzer.utils.RequestPermissions;
import com.street.analyzer.utils.SLog;

import java.io.IOException;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity implements Callback {

    private final String TAG = getClass().getSimpleName();
    private RequestPermissions mRequestPermissions;

    private TextView mTvEmail;
    private Switch mSwRememberMe;
    private TextView mTvPassword;

    private String mEmail, mPassword;

    private Context mContext;
    private CustomOkHttpClient mCustomOkHttpClient;

    private boolean needToRemember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

         mCustomOkHttpClient = new CustomOkHttpClient();

        Intent intent = getIntent();

        if(intent != null) {
            SLog.d(TAG, "Intent not null");
            if (intent.hasExtra(Constants.EXTRA_CREATE_ACCOUNT)) {
                SLog.d(TAG, "opened by create account");
                showExplainMessage();
            } else {
                SharedPreferences sharedPref = this.getSharedPreferences(Constants.USER_DATA, Context.MODE_PRIVATE);
                if(sharedPref.getBoolean(Constants.REMEMBER_ME_STATUS_KEY, false)){
                    String token = sharedPref.getString(Constants.USER_TOKEN_KEY, "");
                    if(token != null && !token.equals("")) {
                        SLog.d(TAG, "Skipping Logging");
                        startMap();
                    }
                }
            }
        }

        mTvEmail = findViewById(R.id.txtEmail);
        mTvPassword = findViewById(R.id.txtPassword);
        mSwRememberMe = findViewById(R.id.switchRememberMe);

        mRequestPermissions = new RequestPermissions(this);
        mContext = getApplicationContext();

        needToRemember = false;

        mSwRememberMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
               needToRemember = isChecked;
               SLog.d(TAG, "Is need to remember: " + isChecked);
            }
        });
        loadingBarStatus(false);

        checkUserPermissions();
    }

    //TODO: Do something to retry the authentication if it fails
    //TODO: Receive the email in another activity and instantiate this after confirm the email
//    private void validateAccount(String token){
//        if(!mCustomOkHttpClient.authenticateAccount(LoginActivity.this, null, token)){
//            SLog.d(TAG, "Start validate account request");
//        }else{
//            //TODO: Do something
//            SLog.d(TAG, "Can't start validate account request");
//        }
//    }

    public void onClickLogin(View v){
        SLog.d(TAG, "onClickLogin");
        mEmail = mTvEmail.getText().toString();
        mPassword = mTvPassword.getText().toString();
        login();
    }

    //TODO: Remove this button
    //In case of server communication failed use this button
    public void onClickSkipLogin(View v){
        SLog.d(TAG, "SkippingLogin");
        startActivity(new Intent(this, MapsActivity.class));
    }

    private void startMap(){
        startActivity(new Intent(this, MapsActivity.class));
    }

    private void login(){
        loadingBarStatus(true);

        SLog.d(TAG, "Sending login request: EMAIL: " + mEmail + " PASSWORD: " + mPassword);
        if(!mCustomOkHttpClient.sendLoginRequest(this, this, mEmail, mPassword)){
            loadingBarStatus(false);
            Toast.makeText(this, Constants.TOAST_NETWORK_NOT_DETECTED, Toast.LENGTH_LONG).show();

            SLog.d(TAG, "Can't login, network error");
        }else{
            SLog.d(TAG, "Start request to server");
        }
    }
    //TODO: Check when user don't consent the permission
    private void checkUserPermissions(){
        SLog.d(TAG, "checkUserPermissions");
        if(mRequestPermissions.isPermissionsGranted()){
            if(!isLocationEnabled()){
                Toast.makeText(this, Constants.TOAST_TURN_ON_LOCATION, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        }else{
            mRequestPermissions.requestUserPermissions(this);
        }
    }

    //Check if location is enable in device
    private boolean isLocationEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
            );
        }
        return false;
    }

    private void parseServerResponse(String response){
        JsonParser jsonParser = new JsonParser();
        HashMap<String, String> s = jsonParser.parseLoginResponse(response);

        SharedPreferences sharedPref = getSharedPreferences(Constants.USER_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        SLog.d(TAG, "Value returned from login: " + s.toString());
        editor.putString(Constants.USER_TOKEN_KEY, s.get(Constants.USER_TOKEN_KEY));
        editor.putString(Constants.USER_NAME_KEY, s.get(Constants.USER_NAME_KEY));
        editor.putString(Constants.USER_EMAIL_KEY, s.get(Constants.USER_EMAIL_KEY));
        //editor.putString(Constants.USER_PICTURE_KEY, s.get(Constants.USER_PICTURE_KEY));
        editor.apply();
    }

    @Override
    public void onFailure(Request request, IOException e) {
        loadingBarStatus(false);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, Constants.TOAST_LOGIN_FAILURE, Toast.LENGTH_LONG).show();
            }
        });
        SLog.d(TAG, "Login - onFailure");
    }

    @Override
    public void onResponse(Response response) throws IOException {
        loadingBarStatus(false);
        if(response.isSuccessful()){
            SLog.d(TAG, "Successfully response");

            String res = response.body().string();
            SLog.d(TAG, "Received: " + res);
            parseServerResponse(res);

            SLog.d(TAG, "Remember password [" + needToRemember + "]");
            setRememberAccount(needToRemember);

            startMap();
            finish();
        }else {
            SLog.d(TAG, "Response fail not successful response ["+response.toString()+"]");
        }
    }

    public void onClickForgotPassword(View v){
        startActivity(new Intent(this, AccountRecoveryCode.class));
    }

    public void onClickLoginReturn(View v){
        finish();
    }

    private void showExplainMessage(){
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setTitle("Success!");
        dlgAlert.setMessage(Constants.ALERT_VERIFY_EMAIL);
        dlgAlert.setPositiveButton(Html.fromHtml("<font color='#9BDE7A'>OK</font>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    //TODO: Save only the email and the auth token
//    private void getRememberAccount(){
//        SLog.d(TAG, "getRememberAccount");
//        SharedPreferences sharedPref = getSharedPreferences(Constants.USER_DATA, Context.MODE_PRIVATE);
//        if(sharedPref.getBoolean(Constants.REMEMBER_ME_STATUS_KEY, false)){
//            mSwRememberMe.setChecked(true);
//            needToRemember = true;
//            mTvEmail.setText(sharedPref.getString(Constants.REMEMBER_ME_EMAIL_KEY, ""));
//            mTvPassword.setText(sharedPref.getString(Constants.REMEMBER_ME_PASSWORD_KEY, ""));
//        }else{
//            SLog.d(TAG, "Remember account false");
//        }
//    }

    private void setRememberAccount(boolean status){
        SLog.d(TAG, "setRememberAccount");
        SharedPreferences sharedPref = getSharedPreferences(Constants.USER_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(Constants.REMEMBER_ME_STATUS_KEY, status);
        editor.apply();
    }

    private void loadingBarStatus(final Boolean status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            findViewById(R.id.loadingPanel).setVisibility((status)?View.VISIBLE:View.GONE);
            mTvEmail.setEnabled(!status);
            mTvPassword.setEnabled(!status);
            mSwRememberMe.setEnabled(!status);
            }
        });
    }
}
