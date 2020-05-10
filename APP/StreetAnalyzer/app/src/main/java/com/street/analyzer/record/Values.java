package com.street.analyzer.record;

import android.location.Location;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;

class Values implements Serializable {

    private ArrayList<Float> mXValue;
    private ArrayList<Float> mYValue;
    private ArrayList<Float> mZValue;

    private ArrayList<Double> mLatitude;
    private ArrayList<Double> mLongitude;

    Values(ArrayList x, ArrayList y, ArrayList z, ArrayList latitude, ArrayList longitude){
        mXValue = x;
        mYValue = y;
        mZValue = z;
        mLatitude = latitude;
        mLongitude = longitude;
    }

}