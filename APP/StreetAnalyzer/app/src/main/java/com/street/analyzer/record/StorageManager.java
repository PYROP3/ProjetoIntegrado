package com.street.analyzer.record;

import android.content.Context;
import android.location.Location;

import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.System.gc;

class StorageManager {

    private final String TAG = getClass().getSimpleName();

    volatile private ArrayList<Float> mXValue;
    volatile private ArrayList<Float> mYValue;
    volatile private ArrayList<Float> mZValue;

    volatile private ArrayList<Double> mLatitude;
    volatile private ArrayList<Double> mLongitude;

    volatile private ArrayList<Integer> mCounter;

    volatile private int mLocationCounter;
    volatile private SaveState mSaveState;
    volatile private boolean isFistTime;
    volatile private int lastValueSize;

    StorageManager(Context context){
        instanceVariables();

        mLocationCounter = 0;
        lastValueSize = 0;
        mSaveState = SaveState.getInstance();
        mSaveState.setContext(context);
        isFistTime = true;
    }

    void registerAccelerometerData(float[] data){
        mXValue.add(data[0]);
        mYValue.add(data[1]);
        mZValue.add(data[2]);
    }

    synchronized void registerPositionChange(double latitude, double longitude){
        mLatitude.add(latitude);
        mLongitude.add(longitude);
        mLocationCounter++;

        if(isFistTime) {
            mSaveState.deleteFile();
            isFistTime = false;
            mXValue.clear();
            mYValue.clear();
            mZValue.clear();
            mCounter.clear();
        }else {
            int val = abs(mXValue.size() - lastValueSize);
            lastValueSize = mXValue.size();
            mCounter.add(val);
        }

        checkLimitPositionChange();
    }

    private synchronized void checkLimitPositionChange(){
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

    private synchronized void instanceVariables(){
        mXValue = new ArrayList<>();
        mYValue = new ArrayList<>();
        mZValue = new ArrayList<>();
        mLatitude = new ArrayList<>();
        mLongitude = new ArrayList<>();
        mCounter = new ArrayList<>();
        gc();
    }

}
