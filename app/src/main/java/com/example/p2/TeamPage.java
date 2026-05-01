package com.example.p2;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TeamPage extends AppCompatActivity {

    private TextView tvTeamTotal, tvTeamUsed;
    private RecyclerView rvMembers;
    private MemberAdapter memberadapter;

    private String teamCode;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_team_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvTeamTotal = findViewById(R.id.tvTeamTotal);
        tvTeamUsed = findViewById(R.id.tvTeamUsed);
        rvMembers = findViewById(R.id.rvMembers);

        teamCode = new SessionManager(this).getUsername();

        loadTeamTotals();
        loadMembers();
    }
    private void loadTeamTotals(){
        db.collection("teams").document(teamCode).get().addOnSuccessListener(doc ->{
            if (doc.exists()){
                Long totalRaw = (Long) doc.get("totalTimeMinutes");
                long total = totalRaw != null ? totalRaw : 0L;

                Long usedRaw = (Long) doc.get("timeUsedMinutes");
                long used = usedRaw != null ? usedRaw : 0L;

                long left = total - used;

                tvTeamTotal.setText("Team Time Left:" + left + " min");
                tvTeamUsed.setText("Total Used:" + used + " min");

                TextView tvCircleText = findViewById(R.id.tvCircleText);
                tvCircleText.setText(left + " min");

                com.google.android.material.progressindicator.CircularProgressIndicator progress = findViewById(R.id.teamProgress);

                int percent = total > 0 ? (int) ((double) used / total * 100) : 0;
                progress.setProgress(percent, true); //animates it
            }
        });
    }
    private void loadMembers(){
        db.collection("teams").document(teamCode).collection("members").get().addOnSuccessListener(query ->{
            List<MemberModel> list = new ArrayList<>();

            for(QueryDocumentSnapshot doc : query) {
            String username = doc.getId();

            //Handles a potential null from Firestore
            Long timeUsedRaw = doc.getLong("timeUsed");
            long timeUsed = timeUsedRaw != null ? timeUsedRaw : 0L;

            @SuppressWarnings("unchecked") //Suppresses the compiler warnings
            List<String> apps = (List<String>) doc.get("appsUsed");

            list.add(new MemberModel(username, timeUsed, apps));
            }
            memberadapter.updateList(list);
        })
                //Adds a log if Firestore fails to load
        .addOnFailureListener(e -> {
            Log.e("loadMembers", "Failed to load members", e);
        });
    }
}