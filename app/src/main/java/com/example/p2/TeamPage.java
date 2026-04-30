package com.example.p2;

import android.os.Bundle;
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

        teamCode = new SessionManager(this).getTeamCode();

        loadTeamTotals();
        loadMembers();
    }
    private void loadTeamTotals(){
        db.collection("teams").document(teamCode).get().addOnSuccessListener(doc ->{
            if (doc.exists()){
                Long total = doc.getLong("totalTimeMinutes");
                Long used = doc.getLong("timeUsedMinutes");

                if (total != null && used != null) {

                    long left = total - used;

                    tvTeamTotal.setText("Team Time Left:" + left + " min");
                    tvTeamUsed.setText("Total Used:" + used + " min");

                    TextView tvCircleText = findViewById(R.id.tvCircleText);
                    tvCircleText.setText(left + " min");

                    com.google.android.material.progressindicator.CircularProgressIndicator progress = findViewById(R.id.teamProgress);

                    int percent = (int) ((double) used / total * 100);

                    progress.setProgress(percent, true);
                }
            }
        });
    }
    private void loadMembers(){
        db.collection("teams").document(teamCode).collection("members").get().addOnSuccessListener(query ->{
            List<MemberModel> list = new ArrayList<>();

            for(QueryDocumentSnapshot doc : query) {
            String username = doc.getId();
            long timeUsed = doc.getLong("timeUsed");
            List<String> apps = (List<String>) doc.get("appsUsed");

            list.add(new MemberModel(username, timeUsed, apps));
            }
            memberadapter.updateList(list);
        });
    }
}