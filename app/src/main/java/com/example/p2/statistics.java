package com.example.p2;

import static com.example.p2.R.*;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
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

public class statistics extends AppCompatActivity {

    public static void setOnClickListener(Object o) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });
        findViewById(R.id.tvNavBlock).setOnClickListener(v -> {
            Intent intent = new Intent(statistics.this, ActivityBlock.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavHome).setOnClickListener(v -> {
            Intent intent = new Intent(statistics.this, ActivityHome.class);
            startActivity(intent);
        });
        findViewById(id.btnCreateTeam).setOnClickListener(v -> {
            Intent intent = new Intent(statistics.this, CreateTeam.class);
            startActivity(intent);
        });
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
}