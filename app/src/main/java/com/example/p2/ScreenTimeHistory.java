package com.example.p2;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;

public class ScreenTimeHistory extends AppCompatActivity {

    private RecyclerView rvHistory;
    private List<Map<String, Object>> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_time_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNav), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
        findViewById(R.id.tvNavHome).setOnClickListener(v -> {
            Intent intent = new Intent(ScreenTimeHistory.this, ActivityHome.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavStatistics).setOnClickListener(v -> {
            Intent intent = new Intent(ScreenTimeHistory.this, statistics.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavBlock).setOnClickListener(v -> {
              Intent intent = new Intent(ScreenTimeHistory.this, ActivityBlock.class);
              startActivity(intent);
        });
        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleGroup);

        loadHistory("total");

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTotal) {
                    loadHistory("total");
                } else if (checkedId == R.id.btnAfterBedtime) {
                    loadHistory("afterBedtime");
                }
            }
        });
    }

    private void loadHistory(String mode) {
        String username = new SessionManager(this).getUsername();

        FirebaseFirestore.getInstance().collection("usernames")
                .document(username)
                .collection("screenTimeHistory")
                .get()
                .addOnSuccessListener(query -> {
                    historyList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("date", doc.getString("date"));
                        entry.put("bedtime", doc.getString("bedtime"));
                        if (mode.equals("total")) {
                            entry.put("minutes", doc.getLong("totalMinutes"));
                            entry.put("label", "Total");
                        } else {
                            entry.put("minutes", doc.getLong("afterBedtimeMinutes"));
                            entry.put("label", "After bedtime");
                        }
                        historyList.add(entry);
                    }
                    setupChart(historyList, mode);
                    rvHistory.setAdapter(new HistoryAdapter(historyList));
                });
    }
    private void setupChart(List<Map<String, Object>> data, String mode) {
        BarChart barChart = findViewById(R.id.historyBarChart);

        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] labels = new String[data.size()];

        for (int i = 0; i < data.size() && i < 7; i++) {
            Map<String, Object> item = data.get(i);
            Long minutes = (Long) item.get("minutes");
            entries.add(new BarEntry(i, minutes != null ? minutes : 0));
            labels[i] = (String) item.get("date");
        }

        BarDataSet dataSet = new BarDataSet(entries, mode.equals("total") ? "Total" : "After Bedtime");
        dataSet.setColor(0xFF00C8A0);
        dataSet.setValueTextColor(0xFFAABBCC);
        dataSet.setValueTextSize(9f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);
        barChart.setBackgroundColor(0xFF1A2B3C);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setTouchEnabled(false);
        barChart.animateY(800);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(0xFFAABBCC);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.invalidate();
    }
}