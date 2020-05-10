package com.street.analyzer.record;

import android.content.Context;
import android.util.Log;

import com.street.analyzer.utils.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

class SaveState {

    private final String TAG = Constants.TAG;

    private File mFolder;
    private Context mContext;

    SaveState(Context context){
        if (mFolder == null) {
            mFolder = context.getExternalFilesDir(null);
        }
        mContext = context;
    }

    void saveData(ArrayList<Values> data) {
        Log.d(TAG, "Trying to load data");
        ObjectOutput out;
        try {
            File outFile = new File(mFolder, Constants.DATA_FILE_NAME);
            out = new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(data);
            out.close();
            Log.d(TAG, "Data saved successfully");
        } catch (Exception e) {
            Log.d(TAG, "Error when trying to save data");
            e.printStackTrace();
        }
    }

    ArrayList<Values> loadData(){
        Log.d(TAG, "Trying to save data");
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (savedData != null) {
            return savedData;
        } else {
           Log.d(TAG, "Error when trying to read data");
            return null;
        }
    }
}
