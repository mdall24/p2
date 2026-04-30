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

import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
        findViewById(id.btnPrevTime).setOnClickListener(v -> {
            Intent intent = new Intent(statistics.this, ScreenTimeHistory.class);
            startActivity(intent);
        });
        String username = new SessionManager(this).getUsername();
        FirebaseFirestore.getInstance().collection("usernames").document(username).get()
                .addOnSuccessListener(doc -> {
                    String teamCode = doc.getString("teamCode");
                    Button btnCreateTeam = findViewById(R.id.btnCreateTeam);
                    if (teamCode != null) {
                        btnCreateTeam.setText("+ Invite Friends");
                        btnCreateTeam.setOnClickListener(v -> {
                            String deepLink = "myapp://join"
                                    + "?team=" + teamCode;

                            String inviteMessage = "Hey! Join my team against screen time!\n" + "Tap to join: " + deepLink;

                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, inviteMessage);
                            startActivity(Intent.createChooser(shareIntent, "Send invite via"));
                        });
                    } else {
                        btnCreateTeam.setText("+     Create Team");
                        btnCreateTeam.setOnClickListener(v -> {
                            Intent intent = new Intent(statistics.this, CreateTeam.class);
                            startActivity(intent);
                        });
                    }
                });
        setupBarChart();
        loadLeaderboard();

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
    private void loadLeaderboard() {
        String username = new SessionManager(this).getUsername();

        FirebaseFirestore.getInstance().collection("usernames").document(username).get()
                .addOnSuccessListener(doc -> {
                    String teamCode = doc.getString("teamCode");
                    if (teamCode == null) return;

                    FirebaseFirestore.getInstance().collection("usernames")
                            .whereEqualTo("teamCode", teamCode)
                            .get()
                            .addOnSuccessListener(query -> {
                                List<Map<String, Object>> members = new ArrayList<>();
                                for (QueryDocumentSnapshot d : query) {
                                    Map<String, Object> member = new HashMap<>();
                                    member.put("username", d.getId());
                                    Long screenTime = d.getLong("screenTime");
                                    member.put("screenTime", screenTime != null ? screenTime : 0L);
                                    members.add(member);
                                }

                                // Sort by lowest screen time first (best time savers)
                                members.sort((a, b) -> Long.compare(
                                        (Long) a.get("screenTime"),
                                        (Long) b.get("screenTime")
                                ));

                                int[] frames = {R.id.firstPlaceFrame, R.id.secondPlaceFrame,
                                        R.id.thirdPlaceFrame, R.id.fourthPlaceFrame, R.id.fifthPlaceFrame};
                                int[] textViews = {R.id.tvFirstPlace, R.id.tvSecondPlace,
                                        R.id.tvThirdPlace, R.id.tvFourthPlace, R.id.tvFifthPlace};

                                for (int i = 0; i < members.size() && i < 5; i++) {
                                    String name = (String) members.get(i).get("username");
                                    long time = (Long) members.get(i).get("screenTime");
                                    String display = name + " • " + time + "m";

                                    findViewById(frames[i]).setVisibility(View.VISIBLE);
                                    ((TextView) findViewById(textViews[i])).setText(display);
                                }
                            });
                });
    }
}