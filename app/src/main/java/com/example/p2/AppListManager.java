package com.example.p2;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class AppListManager {

    private static final String PREFS_NAME = "app_list_prefs";
    private static final String KEY_APPS = "tracked_apps";
    // New — stores which apps are set to soft block
    private static final String KEY_SOFT = "soft_block_apps";

    public static void saveApps(Context context, Set<String> packages) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_APPS, packages).apply();
    }

    public static Set<String> getSavedApps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY_APPS, new HashSet<>()));
    }

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
}