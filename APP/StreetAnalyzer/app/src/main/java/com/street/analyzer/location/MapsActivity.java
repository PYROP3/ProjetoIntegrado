package com.street.analyzer.location;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.navigation.NavigationView;
import com.street.analyzer.R;
import com.street.analyzer.record.RecordService;
import com.street.analyzer.serverCommunication.CustomOkHttpClient;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationButtonClickListener {

    private GoogleMap mMap;

    private Boolean mServiceStats;

    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mNavigationView = findViewById(R.id.nvMenu);
        mServiceStats = false;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
    }

    @Override
    public void onMyLocationClick(@NonNull Location location){
        Toast.makeText(this, "You", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "Going to current location", Toast.LENGTH_SHORT).show();
        return false;
    }

    public void onClickStartRecord(View v){
        if(!mServiceStats){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(this, RecordService.class);
                startService(intent);
            }
        }else{
            stopService(new Intent(this, RecordService.class));
        }
        mServiceStats = !mServiceStats;
    }
}
