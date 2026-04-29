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
}