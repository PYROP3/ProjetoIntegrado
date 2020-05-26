package com.street.analyzer.record;

import android.content.Context;
import android.location.Location;

import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.util.ArrayList;

import static java.lang.System.gc;

class StorageManager {

    private final String TAG = getClass().getSimpleName();

    private ArrayList<Float> mXValue;
    private ArrayList<Float> mYValue;
    private ArrayList<Float> mZValue;

    private ArrayList<Double> mLatitude;
    private ArrayList<Double> mLongitude;

    private ArrayList<Integer> mCounter;

    private int mLocationCounter;
    private SaveState mSaveState;

    StorageManager(Context context){
        instanceVariables();

        mLocationCounter = 0;
        mSaveState = SaveState.getInstance();
        mSaveState.setContext(context);
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

        mCounter.add(mXValue.size());

        checkLimitPositionChange();
    }

    private void checkLimitPositionChange(){
        if(mLocationCounter == Constants.LOCATION_LIMIT_POSITION_CHANGE){
            SLog.d(TAG, "Limit reached, saving new values into storage");
            final Values value = new Values(mXValue, mYValue, mZValue, mLatitude, mLongitude, mCounter);
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
                        SLog.d(TAG, "Device has old data");
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
        mCounter = new ArrayList<>();
        gc();
    }

}
