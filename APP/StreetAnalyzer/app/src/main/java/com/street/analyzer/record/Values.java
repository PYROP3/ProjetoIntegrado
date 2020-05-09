package com.street.analyzer.record;

import android.location.Location;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;

class Values implements Serializable {

    private ArrayList<Float> mXValue = new ArrayList<>();
    private ArrayList<Float> mYValue = new ArrayList<>();
    private ArrayList<Float> mZValue = new ArrayList<>();

    private ArrayList<Double> mLatitude  = new ArrayList<>();
    private ArrayList<Double> mLongitude  = new ArrayList<>();
    private ArrayList<Integer> mNumberItems = new ArrayList<>();

    Values(ArrayList x, ArrayList y, ArrayList z, ArrayList itens
            , ArrayList latitude, ArrayList longitude){
        mXValue = x;
        mYValue = y;
        mZValue = z;
        mNumberItems = itens;
        mLatitude = latitude;
        mLongitude = longitude;
    }

}