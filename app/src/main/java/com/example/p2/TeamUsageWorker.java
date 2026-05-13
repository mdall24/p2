package com.example.p2;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/*Runs every 15 minutes via the WorkManager, which is the minimal interval allowed
 * Calls TeamUsageTracker to update this member's usage and check the team limit.*/
public class TeamUsageWorker extends Worker {

    private static final String TAG = "TeamUsageWorker";
    private static final String WORK_TAG = "team_usage_tracking";

    public TeamUsageWorker(@NonNull Context context, @NonNull WorkerParameters params){
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork(){
        Log.d(TAG, "TeamUsageWorker running");
        TeamUsageTracker.track(getApplicationContext());
        return Result.success();
    }

    /*Uses enqueueUniquePeriodicWork so it won't double schedule if called several times*/
    public static void scheduleIfNeeded(Context context){
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                TeamUsageWorker.class, 15, TimeUnit.MINUTES).addTag(WORK_TAG)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP, request); //Don't restart if already running
        Log.d(TAG, "TeamUsageWorker scheduled");
    }
}
