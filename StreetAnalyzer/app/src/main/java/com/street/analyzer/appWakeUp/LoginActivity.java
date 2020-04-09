package com.street.analyzer.appWakeUp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.street.analyzer.location.MapsActivity;
import com.street.analyzer.R;

public class LoginActivity extends AppCompatActivity {

    private final String TAG = "[TOAST TEST]";
    private RequestPermissions mRequestPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mRequestPermissions = new RequestPermissions(this);
        checkUserPermissions();
    }

    public void temp(View v){
        startActivity(new Intent(this, MapsActivity.class));
    }

    private void checkUserPermissions(){
        if(mRequestPermissions.checkPermission()){
            if(isLocationEnabled()){
                Toast.makeText(this, TAG + "PERMISSION GRANT", Toast.LENGTH_SHORT).show();
            }else{
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
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

}
