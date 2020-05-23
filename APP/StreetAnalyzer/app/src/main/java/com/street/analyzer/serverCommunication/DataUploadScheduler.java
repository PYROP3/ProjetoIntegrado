package com.street.analyzer.serverCommunication;

import android.app.job.JobParameters;
import android.app.job.JobService;

import com.street.analyzer.utils.SLog;

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
                for(int i = 0; i < 10; i++){
                    if(jobStatus)
                        return;
                    SLog.d(TAG, "Run: " + i);
                    try{
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
                SLog.d(TAG, "Job finished");
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
