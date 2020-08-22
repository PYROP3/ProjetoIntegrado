package com.street.analyzer.serverCommunication;

import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.street.analyzer.record.SaveState;
import com.street.analyzer.record.Values;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.io.IOException;


public class DataUploadScheduler extends JobService implements Callback {

    private final String TAG = getClass().getSimpleName();
    private boolean jobStatus = false;
    private SaveState mSaveState;
    JobParameters mJobParameters;

    @Override
    public boolean onStartJob(JobParameters params) {
        SLog.d(TAG, "Job started");

        mJobParameters = params;
        doBackgroundWork();

        return true;
    }

    private void doBackgroundWork(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                SLog.d(TAG, "Starting new scheduled thread");

                mSaveState = SaveState.getInstance();
                Values recordedValues = mSaveState.loadDataMerged();

                uploadLogs(recordedValues);
            }
        }).start();
    }

    private void uploadLogs(Values recordedValues){
        CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();
        customOkHttpClient.sendRegisteredData(this, this, recordedValues);
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        SLog.d(TAG, "Job cancelled before completion");
        jobStatus = true;
        return true;
    }

    private void cancelScheduler(JobParameters params){
        jobFinished(params, false);
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(Constants.JOB_UPLOAD_ID);
    }

    @Override
    public void onFailure(Request request, IOException e) {
        //TODO: Handle onFailure
        SLog.d(TAG, "Job finished error");
    }

    @Override
    public void onResponse(Response response) throws IOException {
        SLog.d(TAG, "Job finished successfully");
        mSaveState.deleteFile();
        cancelScheduler(mJobParameters);
        SLog.d(TAG, "Job cancelled");
    }
}
