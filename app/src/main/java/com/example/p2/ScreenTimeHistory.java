package com.example.p2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScreenTimeHistory extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_time_history);

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        String username = new SessionManager(this).getUsername();

        FirebaseFirestore.getInstance().collection("usernames")
                .document(username)
                .collection("screenTimeHistory")
                .get()
                .addOnSuccessListener(query -> {
                    List<Map<String, Object>> historyList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("date", doc.getString("date"));
                        entry.put("bedtime", doc.getString("bedtime"));
                        entry.put("afterBedtimeMinutes", doc.getLong("afterBedtimeMinutes"));
                        historyList.add(entry);
                    }
                    rvHistory.setAdapter(new HistoryAdapter(historyList));
                });
    }
}