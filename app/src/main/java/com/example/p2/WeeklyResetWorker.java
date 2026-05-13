package com.example.p2;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.units.qual.C;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*Resets every Sunday at midnight. It resets the team's timeUsedMinutes and each
 * individual member's individual usage back to 0, and stops the overlayService
 * so blocked apps become accessible again for the new week.
 *
 * Reschedules for every Sunday after each run*/
public class WeeklyResetWorker extends Worker {

    private static final String TAG = "WeeklyResetWorker";
    private static final String WORK_TAG = "weekly_reset";

    public WeeklyResetWorker(@NonNull Context context, @NonNull WorkerParameters params){
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork(){
        Context context = getApplicationContext();
        Log.d(TAG, "WeeklyResetWorker running - resetting team usage");

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance()
                .getCurrentUser().getUid() : null;
        if (uid == null){
            Log.w(TAG, "No user logged in during reset");
            rescheduleForNextSunday(context);
            return Result.success();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("teams").whereArrayContains("members", uid).get()
                .addOnSuccessListener(query ->{
                    if (query.isEmpty()){
                        Log.w(TAG, "No team found during reset");
                        return;
                    }

                    String teamCode = query.getDocuments().get(0).getId();

                    //Reset the shared team total and all individual member usage
                    Map<String, Object> resetData = new HashMap<>();
                    resetData.put("timeUsedMinutes", 0);
                    resetData.put("memberUsage", new HashMap<>()); //Clears all member entries

                    db.collection("teams").document(teamCode).update(resetData)
                            .addOnSuccessListener(a ->{
                                Log.d(TAG, "Team usage reset to 0");

                                //Stop OverlayService so apps are unblocked for the new week
                                context.stopService(new Intent(context, OverlayService.class));
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to reset team usage", e));
                });

        //Always resets on Sundays, regardless of Firestore results
        rescheduleForNextSunday(context);

        return Result.success();
    }
    /*Calculates the delay until next Sunday at midnight and schedules a new OneTimeWorkRequest.
    * Called on first setup and after each run*/

    public static void scheduleIfNeeded(Context context){
        //Cancels any existing reset workers first to avoid any duplicates
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);
        rescheduleForNextSunday(context);
    }

    private static void rescheduleForNextSunday(Context context){
        Calendar nextSunday = Calendar.getInstance();

        //Find the next Sunday at 00:00
        int daysUntilSunday = (Calendar.SUNDAY - nextSunday.get(Calendar.DAY_OF_WEEK) + 7) % 7;
        if (daysUntilSunday == 0) daysUntilSunday = 7; //If today is Sunday, go to next Sunday

        nextSunday.add(Calendar.DAY_OF_YEAR, daysUntilSunday);
        nextSunday.set(Calendar.HOUR_OF_DAY, 0);
        nextSunday.set(Calendar.MINUTE, 0);
        nextSunday.set(Calendar.SECOND, 0);
        nextSunday.set(Calendar.MILLISECOND, 0);

        long delayMillis = nextSunday.getTimeInMillis() - System.currentTimeMillis();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WeeklyResetWorker.class)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG).build();
        WorkManager.getInstance(context).enqueue(request);

        Log.d(TAG, "WeeklyResetWorker schedules for: " + nextSunday.getTime());
    }

}
