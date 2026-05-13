package com.example.p2;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.checkerframework.checker.units.qual.C;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class BlockScheduleWorker extends Worker {

    private static final String TAG = "BlockScheduleWorker";

    public BlockScheduleWorker(@NonNull Context context, @NonNull WorkerParameters params){
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork(){
        Context context = getApplicationContext();

        //Start the OverlayService
        try {
            Intent serviceIntent = new Intent(context, OverlayService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
            Log.d(TAG, "OverlayService started by BlockScheduleWorker");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start OverlayService", e);
            return Result.failure();
        }

        //Reschedules for same time tomorrow so it repeats itself
        String scheduledTime = getInputData().getString("scheduledTime");
        if (scheduledTime != null && !scheduledTime.isEmpty()){
            rescheduleForTomorrow(context, scheduledTime);
        }
        return Result.success();
    }

    /*Reschedules another one time-worker for same time tomorrow. WorkManager's PeriodicWorkRequest
    * minimum interval is 15 minutes, but doesn't guarantee exact timing, therefore use of
    * OneTimeWorkRequest for more precise daily scheduling*/

    private void rescheduleForTomorrow(Context context, String timeStr){
        try{
            String [] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            tomorrow.set(Calendar.HOUR_OF_DAY, hour);
            tomorrow.set(Calendar.MINUTE, minute);
            tomorrow.set(Calendar.SECOND, 0);
            tomorrow.set(Calendar.MILLISECOND, 0);

            long delayMillis = tomorrow.getTimeInMillis() - System.currentTimeMillis();

            Data inputData = new Data.Builder().putString("scheduledTime", timeStr)
                    .build();

            OneTimeWorkRequest nextRequest = new OneTimeWorkRequest.Builder(BlockScheduleWorker.class)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(inputData).addTag("block_schedule")
                    .build();

            WorkManager.getInstance(context).enqueue(nextRequest);
            Log.d(TAG, "Rescheduled for tomorrow at: " + timeStr);
        } catch (Exception e){
            Log.e(TAG, "Failed to reschedule for tomorrow", e);
        }
    }
}
