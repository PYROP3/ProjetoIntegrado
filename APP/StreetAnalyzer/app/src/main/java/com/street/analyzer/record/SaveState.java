package com.street.analyzer.record;

import android.content.Context;

import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class SaveState {

    private final String TAG = getClass().getSimpleName();

    private File mFolder;
    private Context mContext;

    public SaveState(Context context){
        if (mFolder == null) {
            mFolder = context.getExternalFilesDir(null);
        }
        mContext = context;
    }

    void saveData(ArrayList<Values> data) {
        ObjectOutput out;
        try {
            File outFile = new File(mFolder, Constants.DATA_FILE_NAME);
            out = new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(data);
            out.close();
            SLog.d(TAG, "Data saved successfully");
        } catch (Exception e) {
            SLog.d(TAG, "Error when trying to save data");
            e.printStackTrace();
        }
    }

    ArrayList<Values> loadData(){
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
            e.printStackTrace();
        }
        if (savedData != null) {
            SLog.d(TAG, "Data loaded successfully size: " + savedData.size());
            return savedData;
        } else {
           SLog.d(TAG, "Error when trying to read data");
            return null;
        }
    }

    public long getCurrentDataSize() {
        if (mFolder == null) {
            mFolder = mContext.getExternalFilesDir(null);
        }
        ObjectInput in;
        ArrayList<Values> savedData = null;
        try {
            FileInputStream fileIn = new FileInputStream(mFolder.getPath() + File.separator + Constants.DATA_FILE_NAME);
            return fileIn.getChannel().size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
