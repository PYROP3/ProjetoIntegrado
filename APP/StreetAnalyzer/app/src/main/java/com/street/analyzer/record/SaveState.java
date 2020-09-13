package com.street.analyzer.record;

import android.content.Context;
import android.system.ErrnoException;

import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class SaveState {

    private final String TAG = getClass().getSimpleName();

    private static SaveState instance = null;
    private static int mSavedCounter = 0;

    private File mFolder;
    private Context mContext;

    public static synchronized SaveState getInstance(){
        if(instance == null){
            instance = new SaveState();
        }
        return instance;
    }

    public synchronized void setContext(Context context){
        if (mFolder == null) {
            mFolder = context.getExternalFilesDir(null);
        }
        mContext = context;
    }

    synchronized void saveData(ArrayList<Values> data) {
        ObjectOutput out;
        try {
            File outFile = new File(mFolder, Constants.DATA_FILE_NAME);
            out = new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(data);
            out.close();
            SLog.d(TAG, "Data saved successfully new size: " + data.size());
            mSavedCounter++;
        } catch (Exception e) {
            SLog.d(TAG, "Error when trying to save data");
            e.printStackTrace();
        }
    }

    synchronized ArrayList<Values> loadData(){
        if(mFolder == null){
            mFolder = mContext.getExternalFilesDir(null);
        }
        ObjectInput in;
        ArrayList<Values> savedData = null;
        try {
            FileInputStream fileIn = new FileInputStream(mFolder.getPath() + File.separator + Constants.DATA_FILE_NAME);
            in = new ObjectInputStream(fileIn);
            savedData = (ArrayList<Values>) in.readObject();
            in.close();
        } catch (NullPointerException | IOException | ClassNotFoundException e) {
            SLog.d(TAG, "Deu ruinzao aqui");
        }
        if (savedData != null) {
            SLog.d(TAG, "Data loaded successfully size: " + savedData.size());
            return savedData;
        } else {
           SLog.d(TAG, "Error when trying to read data");
            return null;
        }
    }
    //TODO: Bug can be caused when user record less than necessary to save data into database
    public synchronized Values loadDataMerged(){
        if(mFolder == null){
            mFolder = mContext.getExternalFilesDir(null);
        }
        ObjectInput in;
        ArrayList<Values> savedData = null;
        try {
            FileInputStream fileIn = new FileInputStream(mFolder.getPath() + File.separator + Constants.DATA_FILE_NAME);
            in = new ObjectInputStream(fileIn);
            savedData = (ArrayList<Values>) in.readObject();
            in.close();
        } catch (FileNotFoundException e){
            SLog.d(TAG, "File does not exist");
        } catch (NullPointerException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (savedData != null) {
            SLog.d(TAG, "Data loaded successfully size: " + savedData.size());
            return mergeValues(savedData);
        } else {
            SLog.d(TAG, "Saved data null");
            return null;
        }
    }

    public synchronized long getCurrentDataSize() {
        if (mFolder == null) {
            mFolder = mContext.getExternalFilesDir(null);
        }
        ObjectInput in;
        ArrayList<Values> savedData = null;
        try {
            FileInputStream fileIn = new FileInputStream(mFolder.getPath() + File.separator + Constants.DATA_FILE_NAME);
            long size = fileIn.getChannel().size();
            fileIn.close();
            return size;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public synchronized void deleteFile(){
        if (mFolder == null) {
            mFolder = mContext.getExternalFilesDir(null);
        }
        ObjectInput in;
        ArrayList<Values> savedData = null;
        try {
            File fileIn = new File(mFolder.getPath() + File.separator + Constants.DATA_FILE_NAME);
            boolean ret = fileIn.delete();
            SLog.d(TAG, "File deleted status: " + (ret ? "SUCCESS" : "ERROR"));
            mSavedCounter = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getSavedCounter(){
        return mSavedCounter;
    }

    private Values mergeValues(ArrayList<Values> values){
        Values merged = new Values();
        SLog.d(TAG, "Starting merge values");
        for(Values val : values){
            merged.addAllXValues(val.getXValue());
            merged.addAllYValues(val.getYValue());
            merged.addAllZValues(val.getZValue());
            merged.addAllLatitudes(val.getLatitude());
            merged.addAllLongitudes(val.getLongitude());
            merged.addAllCounters(val.getCounters());
        }

        SLog.d(TAG, "Data successfully merged");
        return merged;
    }
}
