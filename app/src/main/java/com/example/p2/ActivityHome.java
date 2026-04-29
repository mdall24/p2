package com.example.p2;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;

public class ActivityHome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Push content below status bar (clock/camera)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });
        // Bottom nav: Block button
        findViewById(R.id.tvNavBlock).setOnClickListener(v -> {
            Intent intent = new Intent(ActivityHome.this, ActivityBlock.class);
            startActivity(intent);
        });
        // Check if we have permission to read usage stats
        if (!hasUsagePermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }
        // Request overlay permission if not granted
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
        }

        // For testing: Click the time circle or long-press title to set budget
        findViewById(R.id.flGroupTimeCircle).setOnClickListener(v -> showBudgetDialog());
        findViewById(R.id.tvRemainingTitle).setOnLongClickListener(v -> {
            showBudgetDialog();
            return true;
        });


        // Set up the bar chart with real screen time data and update remaining time.
        setupBarChart();
        updateGroupTimeCircle();

    }

    private void showBudgetDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Set daily group budget (hours)");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        int currentBudget = AppListManager.getDailyBudget(this) / 60;
        input.setText(String.valueOf(currentBudget));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                int hours = Integer.parseInt(val);
                AppListManager.saveDailyBudget(this, hours * 60);
                updateGroupTimeCircle(); // Refresh UI immediately
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setupBarChart() {
        BarChart barChart = findViewById(R.id.barChart);

        // Always start from Monday of the current week
        // DAY_OF_WEEK: 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
        Calendar monday = Calendar.getInstance();
        int dayOfWeek = monday.get(Calendar.DAY_OF_WEEK);
        // How many days back is Monday? (if today is Wed=4, we go back 2 days)
        int daysBackToMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        monday.add(Calendar.DAY_OF_YEAR, -daysBackToMonday);
        monday.set(Calendar.HOUR_OF_DAY, 0);
        monday.set(Calendar.MINUTE, 0);
        monday.set(Calendar.SECOND, 0);
        monday.set(Calendar.MILLISECOND, 0);

        // Build one bar per day Mon→Sun, only up to today
        String[] dayLabels = {"M", "T", "W", "T", "F", "S", "S"};
        ArrayList<BarEntry> entries = new ArrayList<>();
        Calendar today = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) monday.clone();
            day.add(Calendar.DAY_OF_YEAR, i);

            if (day.after(today)) {
                entries.add(new BarEntry(i, 0f));
                continue;
            }

            // Pass the actual Calendar day directly — no more daysAgo calculation
            long totalMs = ScreenTimeHelper.getTotalUsageForDay(this, day);
            float totalMinutes = totalMs / 1000f / 60f;
            entries.add(new BarEntry(i, totalMinutes));
            barChart.setTouchEnabled(false);
            barChart.setClickable(false);
        }

        // Create the dataset and style it
        BarDataSet dataSet = new BarDataSet(entries, "Your avg");
        dataSet.setColor(0xFF00C8A0);
        dataSet.setValueTextColor(0xFFAABBCC);
        dataSet.setValueTextSize(9f);

        // Format values as whole minutes e.g. "45m"
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value <= 0) return "";
                int mins = (int) value;
                if (mins >= 60) return (mins / 60) + "h" + (mins % 60 > 0 ? (mins % 60) + "m" : "");
                return mins + "m";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);

        barChart.setBackgroundColor(0xFF0D1B2A);
        barChart.setGridBackgroundColor(0xFF0D1B2A);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.animateY(800);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(0xFFAABBCC);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));

        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);

        barChart.invalidate();
    }

    // This method checks if the user has granted usage stats permission
    private boolean hasUsagePermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void updateGroupTimeCircle() {
        // Get today's total usage across all tracked apps
        Calendar today = Calendar.getInstance();
        long usedMs = ScreenTimeHelper.getTotalUsageForDay(this, today);
        int usedMinutes = (int) (usedMs / 1000 / 60);

        // Get the budget and calculate remaining
        int budgetMinutes = AppListManager.getDailyBudget(this);
        int remainingMinutes = Math.max(0, budgetMinutes - usedMinutes);

        // Format as "1h 30m" or just "45m"
        String display;
        if (remainingMinutes >= 60) {
            int h = remainingMinutes / 60;
            int m = remainingMinutes % 60;
            display = m > 0 ? h + "h " + m + "m" : h + "h";
        } else {
            display = remainingMinutes + "m";
        }

        // Update the TextView in the circle
        TextView tvGroupTime = findViewById(R.id.tvGroupTimeRemaining);
        if (tvGroupTime != null) {
            tvGroupTime.setText(display);
        }
    }
}