package com.street.analyzer.serverCommunication;

import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.street.analyzer.record.SaveState;
import com.street.analyzer.record.Values;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class DataUploadScheduler extends JobService implements Callback {

    private final String TAG = getClass().getSimpleName();
    private boolean jobStatus = false;
    private SaveState mSaveState;
    private JobParameters mJobParameters;

    @Override
    public boolean onStartJob(JobParameters params) {
        SLog.d(TAG, "Job started");

        mJobParameters = params;
        doBackgroundWork();

        return true;
    }

    private void doBackgroundWork(){
        Thread uploadRegisteredData = new Thread(new Runnable() {
            @Override
            public void run() {
                SLog.d(TAG, "Starting new scheduled thread");

                mSaveState = SaveState.getInstance();
                mSaveState.setContext(getApplicationContext());
                Values recordedValues = mSaveState.loadDataMerged();
                SLog.d(TAG, "Number of recorded positions: " + recordedValues.getSize());
                uploadLogs(recordedValues);
            }
        }, "UploadRegisteredData");

        uploadRegisteredData.start();
    }

    private void uploadLogs(Values recordedValues){
        CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();

        customOkHttpClient.sendRegisteredData(this, this, recordedValues,
                                                getUserName(), getUserToken());

    }

    private String getUserName(){
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.USER_DATA, Context.MODE_PRIVATE);
        String s = sharedPreferences.getString(Constants.USER_NAME_KEY, "");
        SLog.d(TAG, "USER NAME: " + s);
        return s;
    }

    private String getUserToken(){
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.USER_DATA, Context.MODE_PRIVATE);
        String s = sharedPreferences.getString(Constants.USER_TOKEN_KEY, "");
        SLog.d(TAG, "USER TOKEN: " + s);
        return s;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        SLog.d(TAG, "Job cancelled before completion");
        jobStatus = true;
        return true;
    }

    @Override
    public void onFailure(Request request, IOException e) {
        jobFinished(mJobParameters, true);
        SLog.d(TAG, "Job finished error");
    }

    @Override
    public void onResponse(Response response) throws IOException {
        SLog.d(TAG, "Job finished");
        if(response.isSuccessful()){
            mSaveState.deleteFile();
            SLog.d(TAG, "All data was sent, unregistering JobScheduler wantsReschedule = FALSE");
            jobFinished(mJobParameters, false);
            SLog.d(TAG, "onResponse: Data sent successfully");
        } else {
            //TODO: It will need to reschedule but not all logs that was already sent
            SLog.d(TAG, "All data was sent, return error");
            jobFinished(mJobParameters, false);
            SLog.d(TAG, "onResponse: ERROR " + response.message());
        }
    }
}
