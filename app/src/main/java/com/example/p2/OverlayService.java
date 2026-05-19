package com.example.p2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "overlay_service";
    private WindowManager windowManager;
    private View overlayView;
    private Handler handler;
    private Runnable checker;

    private String lastBlockedApp = "";
    private boolean overlayShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        startForegroundNotification();
        startChecking();
    }

    private void startForegroundNotification() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Soft Block Service",
                NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Block service running")
                .setContentText("Monitoring apps in the background")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
        } else {
            startForeground(1, notification);
        }
    }

    private void startChecking() {
        checker = new Runnable() {
            @Override
            public void run() {
                checkForegroundApp();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(checker);
    }

    private void checkForegroundApp() {
        // Don't try to show overlay if we don't have permission
        if (!android.provider.Settings.canDrawOverlays(this)) return;

        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
        for (UsageStats us : usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, now - 5000, now)) {
            sortedMap.put(us.getLastTimeUsed(), us);
        }

        if (sortedMap.isEmpty()) return;

        UsageStats lastStats = sortedMap.get(sortedMap.lastKey());
        if (lastStats == null) return;
        String foregroundApp = lastStats.getPackageName();

        if (foregroundApp.equals(getPackageName())) return;

        Set<String> softBlocked = AppListManager.getSoftBlockedApps(this);
        Set<String> hardBlocked = AppListManager.getHardBlockedApps(this);
        boolean pastBedtime = isPastBedtime();

        if (overlayShowing) {
            // If we are showing an overlay, check if we need to switch from Soft to Hard block
            // or if the foreground app has changed
            if (!foregroundApp.equals(lastBlockedApp)) {
                removeOverlay();
            } else if (pastBedtime && hardBlocked.contains(foregroundApp)) {
                // If it's now bedtime and we're only showing the soft popup, switch to hard block
                if (overlayView != null && overlayView.findViewById(R.id.btnHardBlockLeave) == null) {
                    removeOverlay();
                    showHardBlock(foregroundApp);
                }
            }
            return;
        }

        if (!foregroundApp.equals(lastBlockedApp)) {
            if (hardBlocked.contains(foregroundApp)) {
                lastBlockedApp = foregroundApp;
                if (pastBedtime) {
                    showHardBlock(foregroundApp);
                } else {
                    showOverlay(foregroundApp); // Act like soft block before bedtime
                }
            } else if (softBlocked.contains(foregroundApp)) {
                lastBlockedApp = foregroundApp;
                showOverlay(foregroundApp);
            }
        }

        if (!foregroundApp.equals(lastBlockedApp)) {
            lastBlockedApp = "";
        }
    }

    private boolean isPastBedtime() {
        String bedtimeStr = AppListManager.getBedtimeString(this);
        try {
            String[] parts = bedtimeStr.split(":");
            int bedHour = Integer.parseInt(parts[0]);
            int bedMinute = Integer.parseInt(parts[1]);
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);

            // Block from bedtime (e.g. 22:00) until morning (e.g. 06:00)
            boolean afterBedtime = hour > bedHour || (hour == bedHour && minute >= bedMinute);
            boolean beforeMorning = hour < 6;

            return afterBedtime || beforeMorning;
        } catch (Exception e) {
            return false;
        }
    }

    private void showHardBlock(String packageName) {
        overlayShowing = true;

        LayoutInflater inflater = LayoutInflater.from(this);
        overlayView = inflater.inflate(R.layout.overlay_hardblock, null);

        try {
            PackageManager pm = getPackageManager();
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            TextView tvMessage = overlayView.findViewById(R.id.tvHardBlockMessage);
            if (tvMessage != null) {
                tvMessage.setText(pm.getApplicationLabel(info) + " is blocked until morning.");
            }
        } catch (PackageManager.NameNotFoundException e) {
            // keep default
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        params.dimAmount = 0.8f;

        windowManager.addView(overlayView, params);

        View btnLeave = overlayView.findViewById(R.id.btnHardBlockLeave);
        if (btnLeave != null) {
            btnLeave.setOnClickListener(v -> {
                removeOverlay();
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.addCategory(Intent.CATEGORY_HOME);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
            });
        }
    }

    private void showOverlay(String packageName) {
        overlayShowing = true;

        LayoutInflater inflater = LayoutInflater.from(this);
        overlayView = inflater.inflate(R.layout.overlay_popup, null);

        try {
            PackageManager pm = getPackageManager();
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            ((ImageView) overlayView.findViewById(R.id.ivPopupIcon))
                    .setImageDrawable(pm.getApplicationIcon(info));
            ((TextView) overlayView.findViewById(R.id.tvPopupMessage))
                    .setText("Do you want to open " + pm.getApplicationLabel(info) + "?");
        } catch (PackageManager.NameNotFoundException e) {
            ((TextView) overlayView.findViewById(R.id.tvPopupMessage))
                    .setText("Do you want to open this app?");
        }

        // FLAG_NOT_TOUCH_MODAL removed so buttons are tappable
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.dimAmount = 0.6f;

        windowManager.addView(overlayView, params);

        overlayView.findViewById(R.id.btnYes).setOnClickListener(v -> {
            removeOverlay();
        });

        overlayView.findViewById(R.id.btnNo).setOnClickListener(v -> {
            removeOverlay();
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        });
    }

    private void removeOverlay() {
        if (overlayView != null && overlayShowing) {
            windowManager.removeView(overlayView);
            overlayView = null;
            overlayShowing = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checker);
        removeOverlay();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}