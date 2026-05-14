package com.example.p2;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class BlockScheduler {

    private static final String TAG = "BlockScheduler";

    public static void schedule(Context context){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null){
            Log.w(TAG, "No logged-in user, skipping schedule");
            return;
        }

        String uid = user.getUid();

        //Find the team this user belongs to
        FirebaseFirestore.getInstance().collection("teams")
                .whereArrayContains("members", uid).get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()){
                        Log.w(TAG, "No time set for team, skipping schedule");
                        return;
                    }
                    Long suggestedTimeLong = query.getDocuments().get(0).getLong("suggestedTime");
                    if(suggestedTimeLong == null) return;
                    int hours = (int) (suggestedTimeLong / 60);
                    int minutes = (int) (suggestedTimeLong % 60);
                    String timeStr = String.format("%02d:%02d", hours, minutes);
                    scheduleWorker(context, timeStr);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch team for scheduling", e));
    }
    /*Parses HH:MM and schedules BlockScheduleWorker to run at that time.
    * If time had already passed today, it schedules for tomorrow. */
    private static void scheduleWorker(Context context, String timeStr){
        try{
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar target = Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);

            long now = System.currentTimeMillis();

            //If time already passed today, schedule for tomorrow
            if (target.getTimeInMillis() <= now){
                target.add(Calendar.DAY_OF_YEAR, 1);
            }

            long delayMillis = target.getTimeInMillis() - now;

            //Pass the time string to worker so it can reschedule itself
            Data inputData = new Data.Builder().putString("scheduledTime", timeStr)
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(BlockScheduleWorker.class)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(inputData).addTag("block_schedule")
                    .build();

            //Cancel any previously scheduled block work before adding the new one
            WorkManager.getInstance(context).cancelAllWorkByTag("block_schedule");
            WorkManager.getInstance(context).enqueue(workRequest);

            Log.d(TAG, "Block scheduled for: " + target.getTime().toString());
        } catch (Exception e){
            Log.e(TAG, "Failed to parse time or schedule worker: " + timeStr, e);
        }
    }
}
