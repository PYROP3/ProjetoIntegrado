package com.street.analyzer.serverCommunication;

import android.app.job.JobParameters;
import android.app.job.JobService;

import com.street.analyzer.record.SaveState;
import com.street.analyzer.record.Values;
import com.street.analyzer.utils.SLog;

import org.json.JSONObject;

public class DataUploadScheduler extends JobService {

    private final String TAG = getClass().getSimpleName();
    private boolean jobStatus = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        SLog.d(TAG, "Job started");

        doBackgroundWork(params);

        return true;
    }

    private void doBackgroundWork(final JobParameters params){
        new Thread(new Runnable() {
            @Override
            public void run() {
                SLog.d(TAG, "Starting new scheduled thread");

                SaveState saveState = SaveState.getInstance();
                Values recordedValues = saveState.loadDataMerged();
                JsonParser jsonParser = new JsonParser();

                JSONObject jsonObject = jsonParser.createLogToSend(recordedValues);

                SLog.d(TAG, "Job finished");
                saveState.deleteFile();
                //TODO: Set job to not reschedule if the data was uploaded successfully
                jobFinished(params, false);
            }
        }).start();
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        SLog.d(TAG, "Job cancelled before completion");
        jobStatus = true;
        return true;
    }
}
