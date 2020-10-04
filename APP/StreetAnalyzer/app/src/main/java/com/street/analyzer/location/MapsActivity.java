package com.street.analyzer.location;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.material.navigation.NavigationView;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.street.analyzer.R;
import com.street.analyzer.record.RecordService;
import com.street.analyzer.record.SaveState;
import com.street.analyzer.serverCommunication.CustomOkHttpClient;
import com.street.analyzer.serverCommunication.DataUploadScheduler;
import com.street.analyzer.serverCommunication.NetworkStatusManager;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.RequestPermissions;
import com.street.analyzer.utils.SLog;
import com.street.analyzer.wakeUp.AboutUs;
import com.street.analyzer.wakeUp.LoginActivity;

import java.io.IOException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Callback,
        GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationButtonClickListener {

    private final String TAG = getClass().getSimpleName();

    private GoogleMap mMap;

    private Boolean mServiceStats;
    private Context mContext;

    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private NavigationView mNavigationView;
    private boolean pressedOnce;
    private long lastCall;
    private LatLng mLatLng;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;

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
        pressedOnce = false;
        lastCall = 0;
        mContext = getApplicationContext();

        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pressedOnce) {
                    startActivity(new Intent(mContext, LoginActivity.class));
                    finish();
                }
                pressedOnce = true;
                Toast.makeText(mContext, Constants.TOAST_BACK_EXIT, Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pressedOnce = false;
                    }
                }, 2000);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId())
                {
                    case R.id.item_log_out:
                        // LOG_OUT
                        break;
                    case R.id.item_about_us:
                        startActivity(new Intent(mContext, AboutUs.class));
                        break;

                }


                return true;
            }
        });
    }

    @Override
    public void onMyLocationClick(@NonNull Location location){
        Toast.makeText(this, Constants.TOAST_YOU, Toast.LENGTH_LONG).show();
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
    @SuppressLint("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        final RequestPermissions requestPermissions = new RequestPermissions(this);

        if (!requestPermissions.isPermissionsGranted()) {
            requestPermissions.requestUserPermissions(this);
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);

        requestOverlay();

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                final long time = System.currentTimeMillis();
                if(time - lastCall < 2000){
                    return;
                }
                lastCall = time;
                if(mMap.getCameraPosition().zoom > 14) {
                    SLog.d(TAG, "Sending request overlay");
                    requestOverlay();
                }else{
                    SLog.d(TAG, "Minimum zum required!");
                }
            }
        });
    }


    private void requestOverlay(){
        VisibleRegion visibleRegion = mMap.getProjection().getVisibleRegion();
        LatLng nearLeft = visibleRegion.nearLeft;
        LatLng farRight = visibleRegion.farRight;

        SLog.d(TAG, "NLeft latitude: " + nearLeft.latitude + " NLeft longitude: " + nearLeft.longitude);
        SLog.d(TAG, "FRight latitude: " + farRight.latitude + " FRight longitude: " + farRight.longitude);

        mLatLng = new LatLng(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude);

        CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();




        customOkHttpClient.requestQualityOverlay(this, this, nearLeft.latitude,
                nearLeft.longitude, farRight.latitude, farRight.longitude);

//        GroundOverlayOptions overlayOptions = new GroundOverlayOptions()
////                overlayOptions.image(BitmapDescriptorFactory.fromBitmap(bmp));
////                overlayOptions.position(mLatLng, 8600f);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mMap.addGroundOverlay(overlayOptions);
            }
        });
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, Constants.TOAST_CURRENT_LOCATION, Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onDestroy() {
        //TODO: onDestroy is not always called and the app is killed before it
        //TODO: send the end session request
//        SLog.d(TAG, "onDestroy MapsActivity");
//        SharedPreferences sharedPref = this.getSharedPreferences(Constants.USER_DATA, Context.MODE_PRIVATE);
//        if(!sharedPref.getBoolean(Constants.REMEMBER_ME_STATUS_KEY, false)){
//            CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();
//            String token = sharedPref.getString(Constants.USER_TOKEN_KEY, "");
//            if(!token.equals("")) {
//                SLog.d(TAG, "Token != null");
//                customOkHttpClient.sendEndSession(this, token);
//            }else {
//                SLog.d(TAG, "Error trying to end session (empty token)");
//            }
//        }
        super.onDestroy();
    }

    public void onClickStartRecord(View v){
        if(!mServiceStats){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(this, RecordService.class);
                startService(intent);

                Toast.makeText(getApplicationContext(), "Recording Started. Thank you for sharing!", Toast.LENGTH_LONG).show();
            }
        }else{
            stopService(new Intent(this, RecordService.class));
            enableScheduler();
            Toast.makeText(getApplicationContext(), "Recording stopped", Toast.LENGTH_LONG).show();
        }

        mServiceStats = !mServiceStats;
    }

    private void enableScheduler(){
        SLog.d(TAG, "Trying to active job scheduler");
        if(SaveState.getInstance().getSavedCounter() > 0) {
            ComponentName componentName = new ComponentName(this, DataUploadScheduler.class);
            int requiredNetwork = NetworkStatusManager.networkAllowedToSend(this);
            JobInfo jobInfo = null;
            jobInfo = new JobInfo.Builder(Constants.JOB_UPLOAD_ID, componentName)
                    .setRequiredNetworkType(requiredNetwork)
                    .setPersisted(true)
                    .setPeriodic(Constants.JOB_SCHEDULER_PERIOD)
                    .build();
            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            int result = scheduler.schedule(jobInfo);
            SLog.d(TAG, "Scheduler result : " + (result == JobScheduler.RESULT_SUCCESS ? "SUCCESS" : "FAILURE"));
        }
    }

    @Override
    public void onResponse(Response response) throws IOException {
        SLog.d(TAG, "onResponse: ");

//        final Bitmap bmp = BitmapFactory.decodeStream(response.body().byteStream());
////        SLog.d(TAG, "" + BitmapFactory.decodeStream(response.body().byteStream()));
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                GroundOverlayOptions overlayOptions = new GroundOverlayOptions()
//                        .image(BitmapDescriptorFactory.fromBitmap(bmp))
//                        .position(mLatLng, 8600f);
//                mMap.addGroundOverlay(overlayOptions);
//            }
//        });
    }

    @Override
    public void onFailure(Request request, IOException e) {
        SLog.d(TAG, "onFailure: " + request.toString());
    }
}
