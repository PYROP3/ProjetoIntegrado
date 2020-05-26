package com.street.analyzer.record;

import java.io.Serializable;
import java.util.ArrayList;

public class Values implements Serializable {

    private ArrayList<Float> mXValue;
    private ArrayList<Float> mYValue;
    private ArrayList<Float> mZValue;

    private ArrayList<Double> mLatitude;
    private ArrayList<Double> mLongitude;
    private ArrayList<Integer> mCounter;

    Values(){
        mXValue = new ArrayList<>();
        mYValue = new ArrayList<>();
        mZValue = new ArrayList<>();
        mLatitude = new ArrayList<>();
        mLongitude = new ArrayList<>();
        mCounter = new ArrayList<>();
    }

    Values(ArrayList x, ArrayList y, ArrayList z, ArrayList latitude, ArrayList longitude, ArrayList number){
        mXValue = x;
        mYValue = y;
        mZValue = z;
        mLatitude = latitude;
        mLongitude = longitude;
        mCounter = number;
    }

    public ArrayList<Float> getXValue(){return mXValue;}

    public ArrayList<Float> getYValue(){
        return mYValue;
    }

    public ArrayList<Float> getZValue(){return mZValue;}

    public ArrayList<Double> getLatitude(){
        return mLatitude;
    }

    public ArrayList<Double> getLongitude(){
        return mLongitude;
    }

    public ArrayList<Integer> getCounters(){ return mCounter;}

    public void addAllXValues(ArrayList<Float> xValues){
        mXValue.addAll(xValues);
    }

    public void addAllYValues(ArrayList<Float> yValues){
        mYValue.addAll(yValues);
    }

    public void addAllZValues(ArrayList<Float> zValues){
        mZValue.addAll(zValues);
    }

    public void addAllLatitudes(ArrayList<Double> latitudes){
        mLatitude.addAll(latitudes);
    }

    public void addAllLongitudes(ArrayList<Double> longitudes){
        mLongitude.addAll(longitudes);
    }

    public void addAllCoutners(ArrayList<Integer> counters) {mCounter.addAll(counters);}
}