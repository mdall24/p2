package com.example.p2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JoinOrCreateTeam extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_or_create_team);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNav), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });
        findViewById(R.id.tvNavHome).setOnClickListener(v -> {
            Intent intent = new Intent(JoinOrCreateTeam.this, ActivityHome.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavStatistics).setOnClickListener(v -> {
            Intent intent = new Intent(JoinOrCreateTeam.this, statistics.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavBlock).setOnClickListener(v -> {
            Intent intent = new Intent(JoinOrCreateTeam.this, ActivityBlock.class);
            startActivity(intent);
        });
        EditText etTeamCode = findViewById(R.id.etTeamCode);
        Button btnJoinCode = findViewById(R.id.btnJoinCode);
        Button btnCreateTeam = findViewById(R.id.btnCreateTeam);

        btnJoinCode.setOnClickListener(v -> {
            String code = etTeamCode.getText().toString().trim().toUpperCase();
            if (code.isEmpty()) {
                etTeamCode.setError("Please enter a team code");
                etTeamCode.requestFocus();
                return;
            }
            if (code.length() != 6) {
                etTeamCode.setError("Team code must be 6 characters");
                etTeamCode.requestFocus();
                return;
            }

            FirebaseFirestore.getInstance().collection("teams").document(code).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Intent intent = new Intent(JoinOrCreateTeam.this, JoinTeam.class);
                            intent.setData(Uri.parse("myapp://join?team=" + code));
                            startActivity(intent);
                        } else {
                            etTeamCode.setError("Team not found");
                            etTeamCode.requestFocus();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error finding team", Toast.LENGTH_SHORT).show());
        });

        btnCreateTeam.setOnClickListener(v ->
                startActivity(new Intent(JoinOrCreateTeam.this, CreateTeam.class)));

        loadPendingInvites();
    }

    private void loadPendingInvites() {
        String username = new SessionManager(this).getUsername();

        FirebaseFirestore.getInstance().collection("usernames").document(username)
                .collection("invites")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) return;

                    List<Map<String, Object>> inviteList = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : query) {
                        inviteList.add(doc.getData());
                    }

                    findViewById(R.id.cardPendingInvites).setVisibility(View.VISIBLE);

                    RecyclerView rvInvites = findViewById(R.id.rvPendingInvites);
                    rvInvites.setLayoutManager(new LinearLayoutManager(this));
                    rvInvites.setAdapter(new InviteAdapter(inviteList, new InviteAdapter.InviteListener() {
                        @Override
                        public void onAccept(String teamCode) {
                            String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                            FirebaseFirestore.getInstance().collection("teams").document(teamCode)
                                    .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                                    .addOnSuccessListener(a -> {
                                        // Delete the invite
                                        FirebaseFirestore.getInstance().collection("usernames")
                                                .document(username).collection("invites")
                                                .document(teamCode).delete();
                                        Toast.makeText(JoinOrCreateTeam.this, "Joined team!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(JoinOrCreateTeam.this, TeamPage.class));
                                        finish();
                                    });
                        }

                        @Override
                        public void onDecline(String teamCode) {
                            FirebaseFirestore.getInstance().collection("usernames")
                                    .document(username).collection("invites")
                                    .document(teamCode).delete()
                                    .addOnSuccessListener(a -> {
                                        Toast.makeText(JoinOrCreateTeam.this, "Invite declined", Toast.LENGTH_SHORT).show();
                                        loadPendingInvites();
                                    });
                        }
                    }));
                });
    }
}