package com.street.analyzer.wakeUp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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
    private Context mContext;
    private RequestPermissions mRequestPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

         mContext = getApplicationContext();
        mRequestPermissions = new RequestPermissions(mContext);
        checkUserPermissions();
    }

    public void onClickStartMap(View v){
        CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();

        if(!customOkHttpClient.requestJsonTest(mContext, this)){
            Toast.makeText(mContext, "Network not detected"
            + "\nMake sure you are connected to the internet", Toast.LENGTH_LONG).show();

            SLog.d(TAG, "Can't login, network error");
        }else{
            SLog.d(TAG, "Start request to server");
        }
    }

    private void checkUserPermissions(){
        if(mRequestPermissions.checkPermission()){
            if(!isLocationEnabled()){
                Toast.makeText(mContext, "Please, turn on device location", Toast.LENGTH_LONG).show();
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
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onFailure(Request request, IOException e) {
//        Toast.makeText(mContext, "Sorry, we can't loggin!" +
//                "\nFailed to communicate with server", Toast.LENGTH_LONG).show();
        SLog.d(TAG, "Login - onFailure");
    }

    @Override
    public void onResponse(Response response) throws IOException {
        if(response.isSuccessful()){
            SLog.d(TAG, "Successfully response");
            SLog.d(TAG, "Response: " + response.body().string());
            startActivity(new Intent(mContext, MapsActivity.class));
        }else {
            //TODO: Handle the response and check what is the error
            SLog.d(TAG, "Response fail not successful response");
        }
    }

    public void onClickSignUp(View v){
        startActivity(new Intent(mContext, CreateAccountActivity.class));
    }
}
