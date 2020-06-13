package com.street.analyzer.wakeUp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
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
import com.street.analyzer.location.MapsActivity;
import com.street.analyzer.R;
import com.street.analyzer.serverCommunication.CustomOkHttpClient;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity implements Callback {

    private final String TAG = getClass().getSimpleName();
    private RequestPermissions mRequestPermissions;

    private TextView mTvEmail;
    private Switch mSwRememberMe;
    private TextView mTvPassword;

    private boolean needToRemember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if(getIntent().hasExtra(Constants.EXTRA_CREATE_ACCOUNT))
            showExplainMessage();

        mTvEmail = findViewById(R.id.txtEmail);
        mTvPassword = findViewById(R.id.txtPassword);
        mSwRememberMe = findViewById(R.id.switchRememberMe);

        mRequestPermissions = new RequestPermissions(this);

        getRememberAccount();
        needToRemember = false;

        mSwRememberMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
               needToRemember = isChecked;
            }
        });
        loadingBarStatus(false);
    }

    public void onClickLogin(View v){
        login();
    }

    private void login(){
        loadingBarStatus(true);
        checkUserPermissions();

        CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();

        if(!customOkHttpClient.sendLoginRequest(this, this, mTvEmail.getText().toString(), mTvPassword.getText().toString())){
            loadingBarStatus(false);
            Toast.makeText(this, "Network not detected"
                    + "\nMake sure you are connected to the internet", Toast.LENGTH_LONG).show();

            SLog.d(TAG, "Can't login, network error");
        }else{
            SLog.d(TAG, "Start request to server");
        }
    }

    private void checkUserPermissions(){
        if(mRequestPermissions.checkPermission()){
            if(!isLocationEnabled()){
                Toast.makeText(this, "Please, turn on device location", Toast.LENGTH_LONG).show();
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

    @Override
    public void onFailure(Request request, IOException e) {
        loadingBarStatus(false);
        Toast.makeText(this, "Sorry, we can't login!" +
                "\nFailed to communicate with server", Toast.LENGTH_LONG).show();
        SLog.d(TAG, "Login - onFailure");
    }

    @Override
    public void onResponse(Response response) throws IOException {
        loadingBarStatus(false);
        if(response.isSuccessful()){
            SLog.d(TAG, "Successfully response");
            SLog.d(TAG, "Response: " + response.body().string());

            setRememberAccount(needToRemember);

            startActivity(new Intent(this, MapsActivity.class));
        }else {
            //TODO: Handle the response and check what is the error
            SLog.d(TAG, "Response fail not successful response");
        }
    }

    public void onClickForgotPassword(View v){
        startActivity(new Intent(this, CreateAccountActivity.class));
    }

    public void onClickReturn(View v){
        finish();
    }

    private void showExplainMessage(){
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setTitle("Success!");
        dlgAlert.setMessage("Only one more step!\n" +
                "Please check your mail box to verify your email address!");
        dlgAlert.setPositiveButton(Html.fromHtml("<font color='#9BDE7A'>OK</font>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    private void getRememberAccount(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        if(sharedPref.getBoolean(Constants.REMEMBER_ME_STATUS_KEY, false)){
            mSwRememberMe.setChecked(true);
            mTvEmail.setText(sharedPref.getString(Constants.REMEMBER_ME_EMAIL_KEY, ""));
            mTvPassword.setText(sharedPref.getString(Constants.REMEMBER_ME_PASSWORD_KEY, ""));
            login();
        }
    }
    private void setRememberAccount(boolean status){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
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
