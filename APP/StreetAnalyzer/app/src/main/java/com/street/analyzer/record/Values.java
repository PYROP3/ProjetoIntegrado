package com.street.analyzer.record;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

class Values {

    private ArrayList<AccelerometerDataLogger> mAccelerometerData;
    private ArrayList<Integer> mNumberItems;
    private ArrayList<Location> mPositions;

    private AccelerometerDataLogger mAccelerometerValue;
    private int mCounter;

    Values(){
        mAccelerometerData = new ArrayList<>();
        mNumberItems = new ArrayList<>();
        mPositions = new ArrayList<>();

        mAccelerometerValue = new AccelerometerDataLogger();
        mCounter = 0;
    }

     void registerAccelerometerData(float[] data){
        mAccelerometerValue.saveData(data);
        mAccelerometerData.add(mAccelerometerValue);
        mCounter++;
    }

    void registerPositionChange(Location location){
        mPositions.add(location);
        mNumberItems.add(mCounter);
        mCounter = 0;
    }
}

class AccelerometerDataLogger{

    private float[] data = new float[3];

    void saveData(float[] received){
        data[0] = received[0]; data[1] = received[1]; data[2] = received[2];
    }

    float[] getData(){
        return data;
    }
}

