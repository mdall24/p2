package com.example.p2;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScreenTimeHelper {

    // This holds one apps name and how long it was used
    public static class AppUsage {
        public String packageName;
        public long totalTimeMs; // time in milliseconds

        public AppUsage(String packageName, long totalTimeMs) {
            this.packageName = packageName;
            this.totalTimeMs = totalTimeMs;
        }

        // Converts milliseconds into a readable string like "1h 23m"
        public String getReadableTime() {
            long minutes = totalTimeMs / 1000 / 60;
            long hours = minutes / 60;
            minutes = minutes % 60;
            if (hours > 0) return hours + "h " + minutes + "m";
            return minutes + "m";
        }
    }

    // Gets total screen time per app for today
    public static List<AppUsage> getTodayUsage(Context context) {
        UsageStatsManager usageStatsManager =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        // Set time range from midnight today until right now
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        long startTime = startOfDay.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        // Query usage stats for that time range
        Map<String, UsageStats> statsMap = usageStatsManager.queryAndAggregateUsageStats(
                startTime, endTime
        );

        // Convert the map into a simple list, filtering out apps with 0 usage
        List<AppUsage> result = new ArrayList<>();
        for (Map.Entry<String, UsageStats> entry : statsMap.entrySet()) {
            long timeInForeground = entry.getValue().getTotalTimeInForeground();
            if (timeInForeground > 0) {
                result.add(new AppUsage(entry.getKey(), timeInForeground));
            }
        }

        // Sort by most used first
        result.sort((a, b) -> Long.compare(b.totalTimeMs, a.totalTimeMs));

        return result;
    }

    // Gets total screen time across ALL apps for a single day
    // daysAgo: 0 = today, 1 = yesterday, 2 = two days ago etc.
    // Gets total screen time for a specific calendar day
    public static long getTotalUsageForDay(Context context, Calendar targetDay) {
        UsageStatsManager usageStatsManager =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        Calendar start = (Calendar) targetDay.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 1);
        if (end.after(Calendar.getInstance())) {
            end = Calendar.getInstance();
        }

        Map<String, UsageStats> statsMap = usageStatsManager.queryAndAggregateUsageStats(
                start.getTimeInMillis(), end.getTimeInMillis()
        );

        // Get the apps the user has chosen to track
        Set<String> trackedApps = AppListManager.getSavedApps(context);

        // If no apps have been added yet, fall back to counting everything
        if (trackedApps.isEmpty()) {
            return 0;
        }

        long total = 0;
        for (Map.Entry<String, UsageStats> entry : statsMap.entrySet()) {
            if (trackedApps.contains(entry.getKey())) {
                android.util.Log.d("SCREENTIME", "Counting: " + entry.getKey() + " → " + (entry.getValue().getTotalTimeInForeground()/1000/60) + "m");
                total += entry.getValue().getTotalTimeInForeground();
            }
        }
        return total;
    }
    public static long getUsageForTimeRange(Context context, long startTime, long endTime) {
        UsageStatsManager usageStatsManager =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        Map<String, UsageStats> statsMap = usageStatsManager.queryAndAggregateUsageStats(
                startTime, endTime
        );

        Set<String> trackedApps = AppListManager.getSavedApps(context);

        if (trackedApps.isEmpty()) return 0;

        long total = 0;
        for (Map.Entry<String, UsageStats> entry : statsMap.entrySet()) {
            if (trackedApps.contains(entry.getKey())) {
                total += entry.getValue().getTotalTimeInForeground();
            }
        }
        return total;
    }
}