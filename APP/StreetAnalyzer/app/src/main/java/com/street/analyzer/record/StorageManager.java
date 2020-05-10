package com.street.analyzer.record;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.street.analyzer.utils.Constants;

import java.util.ArrayList;

import static java.lang.System.gc;

class StorageManager {

    private final String TAG = Constants.TAG;

    private ArrayList<Float> mXValue;
    private ArrayList<Float> mYValue;
    private ArrayList<Float> mZValue;

    private ArrayList<Double> mLatitude;
    private ArrayList<Double> mLongitude;

    private int mLocationCounter;
    private SaveState mSaveState;

    StorageManager(Context context){
        instanceVariables();

        mLocationCounter = 0;
        mSaveState = new SaveState(context);
    }

    void registerAccelerometerData(float[] data){
        mXValue.add(data[0]);
        mYValue.add(data[1]);
        mZValue.add(data[2]);
    }

    void registerPositionChange(double latitude, double longitude){
        mLatitude.add(latitude);
        mLongitude.add(longitude);
        mLocationCounter++;

        checkLimitPositionChange();
        Log.d(TAG, "Position change registered");
    }

    private void checkLimitPositionChange(){
        if(mLocationCounter == Constants.LOCATION_LIMIT_POSITION_CHANGE){
            Log.d(TAG, "Limit reached, saving new values into storage");
            final Values value = new Values(mXValue, mYValue, mZValue, mLatitude, mLongitude);
            instanceVariables();
            mLocationCounter = 0;

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ArrayList<Values> newData = new ArrayList<>();
                    newData.add(value);

                    ArrayList<Values> oldData = mSaveState.loadData();
                    if(oldData != null){
                        newData.addAll(oldData);
                        Log.d(TAG, "Device has old data");
                    }
                    mSaveState.saveData(newData);
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }

    private void instanceVariables(){
        mXValue = new ArrayList<>();
        mYValue = new ArrayList<>();
        mZValue = new ArrayList<>();
        mLatitude = new ArrayList<>();
        mLongitude = new ArrayList<>();
        gc();
    }

}
