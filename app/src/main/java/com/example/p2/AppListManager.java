package com.example.p2;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class AppListManager {

    // The name of our SharedPreferences file
    private static final String PREFS_NAME = "app_list_prefs";
    // The key we store the set of packages under
    private static final String KEY_APPS = "tracked_apps";

    // Save a set of package names
    public static void saveApps(Context context, Set<String> packages) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_APPS, packages).apply();
    }

    // Load the saved set of package names
    public static Set<String> getSavedApps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Returns empty set if nothing saved yet
        return new HashSet<>(prefs.getStringSet(KEY_APPS, new HashSet<>()));
    }

    // Check if any apps have been added yet
    public static boolean hasApps(Context context) {
        return !getSavedApps(context).isEmpty();
    }

    // Save which apps are soft blocked
    public static void setSoftBlock(Context context, String packageName, boolean isSoft) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> softApps = new HashSet<>(prefs.getStringSet(KEY_SOFT, new HashSet<>()));
        if (isSoft) {
            softApps.add(packageName);
        } else {
            softApps.remove(packageName);
        }
        prefs.edit().putStringSet(KEY_SOFT, softApps).apply();
    }

    // Check if a specific app is soft blocked
    public static boolean isSoftBlocked(Context context, String packageName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> softApps = prefs.getStringSet(KEY_SOFT, new HashSet<>());
        return softApps.contains(packageName);
    }

    // Get all soft blocked apps
    public static Set<String> getSoftBlockedApps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY_SOFT, new HashSet<>()));
    }

    // Save the daily budget in minutes
    public static void saveDailyBudget(Context context, int minutes) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt("daily_budget_minutes", minutes).apply();
    }

    // Get the daily budget in minutes (default 3 hours = 180 minutes)
    public static int getDailyBudget(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt("daily_budget_minutes", 180);
    }

}