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
        findViewById(R.id.btnPrevTime).setOnClickListener(v -> {
            Intent intent = new Intent(statistics.this, ScreenTimeHistory.class);
            startActivity(intent);
        });
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("teams")
                .whereArrayContains("members", currentUid)
                .get()
                .addOnSuccessListener(query -> {
                    String teamCode = query.isEmpty() ? null : query.getDocuments().get(0).getId();
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
        String username = new SessionManager(this).getUsername();
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        // First get your own screen time history
        FirebaseFirestore.getInstance().collection("usernames")
                .document(username)
                .collection("screenTimeHistory")
                .get()
                .addOnSuccessListener(myQuery -> {
                    Map<String, Long> myDateMap = new HashMap<>();
                    for (QueryDocumentSnapshot doc : myQuery) {
                        String date = doc.getString("date");
                        Long minutes = doc.getLong("totalMinutes");
                        if (date != null && minutes != null) {
                            myDateMap.put(date, minutes);
                        }
                    }

                    // Then get team members
                    FirebaseFirestore.getInstance().collection("teams")
                            .whereArrayContains("members", uid)
                            .get()
                            .addOnSuccessListener(teamQuery -> {
                                if (teamQuery.isEmpty()) {
                                    buildChart(barChart, myDateMap, null);
                                    return;
                                }

                                List<String> memberUids = (List<String>) teamQuery.getDocuments().get(0).get("members");
                                if (memberUids == null || memberUids.size() <= 1) {
                                    buildChart(barChart, myDateMap, null);
                                    return;
                                }

                                // Fetch all members' screen time history
                                Map<String, List<Long>> groupDateMap = new HashMap<>();
                                int[] loadedCount = {0};
                                int totalMembers = memberUids.size();

                                for (String memberUid : memberUids) {
                                    FirebaseFirestore.getInstance().collection("usernames")
                                            .whereEqualTo("uid", memberUid)
                                            .get()
                                            .addOnSuccessListener(userQuery -> {
                                                if (!userQuery.isEmpty()) {
                                                    String memberUsername = userQuery.getDocuments().get(0).getId();
                                                    FirebaseFirestore.getInstance().collection("usernames")
                                                            .document(memberUsername)
                                                            .collection("screenTimeHistory")
                                                            .get()
                                                            .addOnSuccessListener(historyQuery -> {
                                                                for (QueryDocumentSnapshot doc : historyQuery) {
                                                                    String date = doc.getString("date");
                                                                    Long minutes = doc.getLong("totalMinutes");
                                                                    if (date != null && minutes != null) {
                                                                        if (!groupDateMap.containsKey(date)) {
                                                                            groupDateMap.put(date, new ArrayList<>());
                                                                        }
                                                                        groupDateMap.get(date).add(minutes);
                                                                    }
                                                                }
                                                                loadedCount[0]++;
                                                                if (loadedCount[0] == totalMembers) {
                                                                    buildChart(barChart, myDateMap, groupDateMap);
                                                                }
                                                            });
                                                } else {
                                                    loadedCount[0]++;
                                                    if (loadedCount[0] == totalMembers) {
                                                        buildChart(barChart, myDateMap, groupDateMap);
                                                    }
                                                }
                                            });
                                }
                            });
                });
    }

    private void buildChart(BarChart barChart, Map<String, Long> myDateMap, Map<String, List<Long>> groupDateMap) {
        ArrayList<BarEntry> myEntries = new ArrayList<>();
        ArrayList<BarEntry> groupEntries = new ArrayList<>();
        String[] dayLabels = {"M", "T", "W", "T", "F", "S", "S"};

        Calendar monday = Calendar.getInstance();
        int dayOfWeek = monday.get(Calendar.DAY_OF_WEEK);
        int daysBackToMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        monday.add(Calendar.DAY_OF_YEAR, -daysBackToMonday);
        monday.set(Calendar.HOUR_OF_DAY, 0);
        monday.set(Calendar.MINUTE, 0);
        monday.set(Calendar.SECOND, 0);
        monday.set(Calendar.MILLISECOND, 0);

        Calendar today = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) monday.clone();
            day.add(Calendar.DAY_OF_YEAR, i);

            if (day.after(today)) {
                myEntries.add(new BarEntry(i, 0f));
                groupEntries.add(new BarEntry(i, 0f));
                continue;
            }

            String dateKey = day.get(Calendar.YEAR) + "-"
                    + (day.get(Calendar.MONTH) + 1) + "-"
                    + day.get(Calendar.DAY_OF_MONTH);

            Long myMinutes = myDateMap.get(dateKey);
            myEntries.add(new BarEntry(i, myMinutes != null ? myMinutes : 0f));

            if (groupDateMap != null) {
                List<Long> groupValues = groupDateMap.get(dateKey);
                if (groupValues != null && !groupValues.isEmpty()) {
                    long sum = 0;
                    for (Long v : groupValues) sum += v;
                    groupEntries.add(new BarEntry(i, (float) sum / groupValues.size()));
                } else {
                    groupEntries.add(new BarEntry(i, 0f));
                }
            }
        }

        BarDataSet myDataSet = new BarDataSet(myEntries, "Your Total");
        myDataSet.setColor(0xFF00C8A0);
        myDataSet.setValueTextColor(0xFFAABBCC);
        myDataSet.setValueTextSize(9f);

        com.github.mikephil.charting.formatter.ValueFormatter formatter = new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value <= 0) return "";
                int mins = (int) value;
                if (mins >= 60) return (mins / 60) + "h" + (mins % 60 > 0 ? (mins % 60) + "m" : "");
                return mins + "m";
            }
        };

        myDataSet.setValueFormatter(formatter);

        BarData barData;
        if (groupDateMap != null) {
            BarDataSet groupDataSet = new BarDataSet(groupEntries, "Group Total");
            groupDataSet.setColor(0xFFF4A430);
            groupDataSet.setValueTextColor(0xFFAABBCC);
            groupDataSet.setValueTextSize(9f);
            groupDataSet.setValueFormatter(formatter);
            barData = new BarData(myDataSet, groupDataSet);
            barData.setBarWidth(0.35f);
        } else {
            barData = new BarData(myDataSet);
            barData.setBarWidth(0.5f);
        }

        barChart.setData(barData);

        if (groupDateMap != null) {
            barChart.groupBars(0, 0.1f, 0.05f);
        }

        barChart.setBackgroundColor(0xFF0D1B2A);
        barChart.setGridBackgroundColor(0xFF0D1B2A);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setTouchEnabled(false);
        barChart.animateY(800);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(0xFFAABBCC);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(7f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        xAxis.setCenterAxisLabels(true);

        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.invalidate();
    }
    private void loadLeaderboard() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("teams")
                .whereArrayContains("members", uid)
                .get()
                .addOnSuccessListener(teamQuery -> {
                    if (teamQuery.isEmpty()) return;

                    String teamCode = teamQuery.getDocuments().get(0).getId();
                    List<String> memberUids = (List<String>) teamQuery.getDocuments().get(0).get("members");

                    if (memberUids == null) return;

                    List<Map<String, Object>> members = new ArrayList<>();

                    for (String memberUid : memberUids) {
                        FirebaseFirestore.getInstance().collection("usernames")
                                .whereEqualTo("uid", memberUid)
                                .get()
                                .addOnSuccessListener(userQuery -> {
                                    if (!userQuery.isEmpty()) {
                                        String username = userQuery.getDocuments().get(0).getId();
                                        Long screenTime = userQuery.getDocuments().get(0).getLong("ScreenTime");
                                        Map<String, Object> member = new HashMap<>();
                                        member.put("username", username);
                                        member.put("screenTime", screenTime != null ? screenTime : 0L);
                                        members.add(member);

                                        if (members.size() == memberUids.size()) {
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
                                                String display;
                                                if (time >= 60) {
                                                    long hours = time / 60;
                                                    long mins = time % 60;
                                                    display = name + " • " + hours + "h " + mins + "m";
                                                } else {
                                                    display = name + " • " + time + "m";
                                                }
                                                findViewById(frames[i]).setVisibility(View.VISIBLE);
                                                ((TextView) findViewById(textViews[i])).setText(display);
                                            }
                                        }
                                    }
                                });
                    }
                });
    }
}