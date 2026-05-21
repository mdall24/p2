package com.example.p2;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.checkerframework.checker.units.qual.C;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*Purpose of this is to track team's usage via a UsageStatsManager, only filtered to the teams
 agreed apps. It finds them in Firestore in suggestedApps, takes the sum of the members
 and checks if it hits the team-limit and starts the overlayservice if it is hit. It's called every
 15 minutes*/
public class TeamUsageTracker {
    private static final String TAG = "TeamUsageTracker";

    public static void track(Context context){
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null){
            Log.w(TAG, "No logged-in user, skipping track");
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //This will find the user's team
        db.collection("teams").whereArrayContains("members", uid).get()
                .addOnSuccessListener(query ->{
                    if(query.isEmpty()){
                        Log.w(TAG, "User has no team");
                        return;
                    }
                    String teamCode = query.getDocuments().get(0).getId();
                    Map<String, Object> teamData = query.getDocuments().get(0).getData();

                    //Gets the agreed list of apps to block
                    List<String> agreedApps = (List<String>) teamData.get("suggestedApps");
                    if (agreedApps == null || agreedApps.isEmpty()){
                        Log.w(TAG, "No agreed apps for team");
                        return;
                    }

                    Long totalMinutes = query.getDocuments().get(0).getLong("suggestedTime");
                    if (totalMinutes == null || totalMinutes <= 0){
                        Log.w(TAG, "No total time set for team");
                        return;
                    }

                    //This calculates this user's usage of agreed apps since Monday
                    long usageMinutes = ScreenTimeHelper.getWeeklyUsageForPackages(context, agreedApps);
                    Log.d(TAG, "This member's weekly usage of team apps: " + usageMinutes + " min");

                    //Writes member's usage to Firestore under the teamdoc(?)
                    Map<String, Object> usageUpdate = new HashMap<>();
                    usageUpdate.put("memberUsage." + uid, usageMinutes);

                    db.collection("teams").document(teamCode)
                            .set(usageUpdate, SetOptions.merge())
                            .addOnSuccessListener(a -> {
                                Log.d(TAG, "Updated usage for " + uid + ": " + usageMinutes + " min");
                                //Gets the list used by the member
                                List<String> usedAppNames = ScreenTimeHelper.getUsedAppNames(context, agreedApps);

                                Map<String, Object> userUpdate = new HashMap<>();
                                userUpdate.put("weeklyMinutes", usageMinutes);
                                userUpdate.put("dailyMinutes", ScreenTimeHelper.getDailyUsageForPackages(context, agreedApps));
                                userUpdate.put("timeUsedMinutes", usageMinutes);
                                userUpdate.put("usedApps", usedAppNames);

                                db.collection("members").document(uid)
                                                .set(userUpdate, SetOptions.merge())
                                                        .addOnSuccessListener(b -> Log.d(TAG, "Updated user stats for " + uid))
                                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update user stats", e));

                                //Re-reads all members' usage and sums them
                                db.collection("teams").document(teamCode).get()
                                        .addOnSuccessListener(doc -> {
                                            Map<String, Object> memberUsage = (Map<String, Object>) doc.get("memberUsage");

                                            long teamTotal = 0;
                                            if(memberUsage != null){
                                                for (Object val : memberUsage.values()){
                                                    if (val instanceof Long) teamTotal += (Long) val;
                                                    else if(val instanceof Double) teamTotal += ((Double) val).longValue();
                                                }
                                            }
                                            //Saves the summed total back to Firestore
                                            db.collection("teams").document(teamCode).update("timeUsedMinutes", teamTotal);
                                            Log.d(TAG, "Team total used: " + teamTotal + " / " + totalMinutes + " min");

                                            //Checks if limit is hit
                                            if (teamTotal >= totalMinutes){
                                                Log.d(TAG, "Team limit reached - starting OverlayService");

                                                //Saves agreed apps to AppsListManager, so OverlayService knows which apps to block
                                                AppListManager.saveApps(context, new java.util.HashSet<>(agreedApps));
                                                Intent serviceIntent = new Intent(context, OverlayService.class);
                                                ContextCompat.startForegroundService(context, serviceIntent);
                                            }
                                            else {
                                                //If limit is not reached makes sure service is stopped (after reset on Sundays), stops the service
                                                context.stopService(new Intent(context, OverlayService.class));
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update memberUsage", e));
                });
    }
}
