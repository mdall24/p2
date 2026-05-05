package com.example.p2;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamPage extends AppCompatActivity {

    private TextView tvTeamTotal, tvTeamUsed;
    private RecyclerView rvMembers;
    private MemberAdapter memberadapter;

    private Button btnAddApps, btnRemoveApps;

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
        findViewById(R.id.tvNavHome).setOnClickListener(v -> {
            Intent intent = new Intent(TeamPage.this, ActivityHome.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavStatistics).setOnClickListener(v -> {
            Intent intent = new Intent(TeamPage.this, statistics.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavBlock).setOnClickListener(v -> {
            Intent intent = new Intent(TeamPage.this, ActivityBlock.class);
            startActivity(intent);
        });

        tvTeamTotal = findViewById(R.id.tvTeamTotal);
        tvTeamUsed = findViewById(R.id.tvTeamUsed);
        rvMembers = findViewById(R.id.rvMembers);
        btnAddApps = findViewById(R.id.btnAddApps);
        btnRemoveApps = findViewById(R.id.btnRemoveApps);
        ImageButton btnOptions = findViewById(R.id.btnTeamOptions);

        memberadapter = new MemberAdapter(new ArrayList<>());
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(memberadapter);

        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("teams")
                .whereArrayContains("members", uid)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        teamCode = query.getDocuments().get(0).getId();
                        loadTeamTotals();
                        loadMembers();
                    } else {
                        Toast.makeText(this, "No team found. Please join or create a team.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

        btnOptions.setOnClickListener(v -> {
            View bottomSheetView = getLayoutInflater().inflate(R.layout.team_options, null);
            BottomSheetDialog bottomSheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
            bottomSheet.setContentView(bottomSheetView);


            TextView btnKickMember = bottomSheetView.findViewById(R.id.btnKickMember);
            TextView btnDisbandTeam = bottomSheetView.findViewById(R.id.btnDisbandTeam);
            View dividerDisband = bottomSheetView.findViewById(R.id.dividerDisband);
            TextView btnLeaveTeam = bottomSheetView.findViewById(R.id.btnLeaveTeam);
            TextView btnInviteMember = bottomSheetView.findViewById(R.id.btnInviteMember);

            String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("teams").document(teamCode).get()
                    .addOnSuccessListener(doc -> {
                        String createdBy = doc.getString("createdBy");
                        boolean isLeader = currentUid.equals(createdBy);

                        if (isLeader) {
                            btnKickMember.setVisibility(View.VISIBLE);
                            btnDisbandTeam.setVisibility(View.VISIBLE);
                            dividerDisband.setVisibility(View.VISIBLE);
                        }
                    });

            btnLeaveTeam.setOnClickListener(view -> {
                bottomSheet.dismiss();
                showLeaveConfirmation();
            });

            btnInviteMember.setOnClickListener(view -> {
                bottomSheet.dismiss();
                showInviteByUsernameDialog();
            });

            btnKickMember.setOnClickListener(view -> {
                bottomSheet.dismiss();
                showKickMemberDialog();
            });

            btnDisbandTeam.setOnClickListener(view -> {
                bottomSheet.dismiss();
                showDisbandConfirmation();
            });
            bottomSheet.show();
        });
        btnAddApps.setOnClickListener(v -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            List<String> appNames = new ArrayList<>();
            for (ApplicationInfo app : installedApps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    appNames.add(pm.getApplicationLabel(app).toString());
                }
            }
            Collections.sort(appNames);

            String[] appArray = appNames.toArray(new String[0]);

            new AlertDialog.Builder(this).setTitle("Suggest an app").setItems(appArray, (dialog, which) -> {
                        String selectedApps = appArray[which];
                        suggestApp(selectedApps);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).show();
        });

        btnRemoveApps.setOnClickListener(v -> {
            db.collection("teams").document(teamCode).get()
                    .addOnSuccessListener(doc -> {
                        List<String> suggestedApps = (List<String>) doc.get("suggestedApps");

                        if (suggestedApps == null || suggestedApps.isEmpty()) {
                            new AlertDialog.Builder(this)
                                    .setTitle("No apps to remove")
                                    .setMessage("There are no suggested apps in the list yet.")
                                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                    .show();
                            return;
                        }
                        String[] appArray = suggestedApps.toArray(new String[0]);

                        new AlertDialog.Builder(this)
                                .setTitle("Suggest removing an app")
                                .setItems(appArray, (dialog, which) -> {
                                    String selectedApp = appArray[which];
                                    suggestRemoveApp(selectedApp);
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .show();
                    })
                    .addOnFailureListener(e -> Log.e("btnRemoveApps", "Failed to load apps", e));
        });
    }

    private void showLeaveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Team")
                .setMessage("Are you sure you want to leave the team?")
                .setPositiveButton("Leave", (dialog, which) -> {
                    //Handles the leave action
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showKickMemberDialog() {
        db.collection("teams").document(teamCode).get()
                .addOnSuccessListener(doc -> {
                    List<String> memberUids = (List<String>) doc.get("members");
                    if (memberUids == null || memberUids.isEmpty()) {
                        Toast.makeText(this, "No members found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                    List<String> otherUids = new ArrayList<>();
                    for (String memberUid : memberUids) {
                        if (!memberUid.equals(currentUid)) {
                            otherUids.add(memberUid);
                        }
                    }

                    if (otherUids.isEmpty()) {
                        Toast.makeText(this, "No other members to kick", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> usernames = new ArrayList<>();
                    for (String memberUid : otherUids) {
                        db.collection("usernames").whereEqualTo("uid", memberUid).get()
                                .addOnSuccessListener(userQuery -> {
                                    if (!userQuery.isEmpty()) {
                                        usernames.add(userQuery.getDocuments().get(0).getId());
                                    } else {
                                        usernames.add(memberUid);
                                    }

                                    if (usernames.size() == otherUids.size()) {
                                        String[] namesArray = usernames.toArray(new String[0]);
                                        new AlertDialog.Builder(this).setTitle("Kick a Member")
                                                .setItems(namesArray, (dialog, which) -> {
                                                    String kickedUid = otherUids.get(which);
                                                    new AlertDialog.Builder(this)
                                                            .setTitle("Kick " + usernames.get(which) + "?")
                                                            .setMessage("Are you sure you want to kick " + usernames.get(which) + "?")
                                                            .setPositiveButton("Kick", (d, w) -> kickMember(kickedUid))
                                                            .setNegativeButton("Cancel", null)
                                                            .show();
                                                })
                                                .show();
                                    }
                                });
                    }
                });
    }

    private void showInviteByUsernameDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter username");

        new AlertDialog.Builder(this)
                .setTitle("Invite by Username")
                .setView(input)
                .setPositiveButton("Send Invite", (dialog, which) -> {
                    String invitedUsername = input.getText().toString().trim();
                    if (invitedUsername.isEmpty()) return;

                    // Check if user exists
                    db.collection("usernames").document(invitedUsername).get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Save invite to their document
                                java.util.Map<String, Object> invite = new java.util.HashMap<>();
                                invite.put("teamCode", teamCode);
                                invite.put("teamName", doc.getString("name"));
                                invite.put("from", new SessionManager(this).getUsername());

                                db.collection("usernames").document(invitedUsername)
                                        .collection("invites").document(teamCode)
                                        .set(invite)
                                        .addOnSuccessListener(a ->
                                                Toast.makeText(this, "Invite sent to " + invitedUsername, Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to send invite", Toast.LENGTH_SHORT).show());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void kickMember(String uid) {
        db.collection("teams").document(teamCode)
                .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(uid))
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Member kicked", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to kick member", Toast.LENGTH_SHORT).show());
    }

    private void showDisbandConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Disband Team?")
                .setMessage("This will permanently disband team for everyone. Are you sure?")
                .setPositiveButton("Disband", (dialog, which) -> {
                    db.collection("teams").document(teamCode).delete().addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Team Disbanded", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(TeamPage.this, ActivityHome.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to disband team", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void suggestApp(String appName) {
        String currentUsername = new SessionManager(this).getUsername();

        Map<String, Object> pending = new HashMap<>();
        pending.put("proposedBy", currentUsername);
        pending.put("approvals", Collections.singletonList(currentUsername));
        pending.put("rejections", new ArrayList<>());

        db.collection("teams").document(teamCode)
                .update("pendingApps." + appName, pending)
                .addOnSuccessListener(a -> Log.d("suggestApp", appName + "suggested"))
                .addOnFailureListener(e -> Log.e("suggestApp", "Failed", e));
    }

    private void suggestRemoveApp(String appName) {
        String currentUsername = new SessionManager(this).getUsername();

        Map<String, Object> pending = new HashMap<>();
        pending.put("proposedBy", currentUsername);
        pending.put("approvals", Collections.singletonList(currentUsername));
        pending.put("rejections", new ArrayList<>());
        pending.put("action", "remove"); //marks as removal suggestion

        db.collection("teams").document(teamCode)
                .update(("pendingApps.") + appName, pending)
                .addOnSuccessListener(a -> Log.d("suggestRemoveApp", appName + " removal suggested"))
                .addOnFailureListener(e -> Log.e("suggestRemoveApp", "Failed", e));
    }

    private void loadTeamTotals() {
        db.collection("teams").document(teamCode).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {

                        String teamName = doc.getString("name");
                        TextView tvTeamName = findViewById(R.id.TeamPage);
                        tvTeamName.setText(teamName != null ? teamName : "No Name");

                        Long totalRaw = (Long) doc.get("totalTimeMinutes");
                        long total = totalRaw != null ? totalRaw : 0L;

                        Long usedRaw = (Long) doc.get("timeUsedMinutes");
                        long used = usedRaw != null ? usedRaw : 0L;

                        long left = total - used;

                        tvTeamTotal.setText("Team Time Left: " + left + " min");
                        tvTeamUsed.setText("Total Used: " + used + " min");

                        TextView tvCircleText = findViewById(R.id.tvCircleText);
                        tvCircleText.setText(left + " min");

                        com.google.android.material.progressindicator.CircularProgressIndicator progress = findViewById(R.id.teamProgress);

                        int percent = total > 0 ? (int) ((double) used / total * 100) : 0;
                        progress.setProgress(percent, true); //animates it
                    }
                });
    }

    private void loadMembers() {
        db.collection("teams").document(teamCode)
                .get().addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    List<String> memberUids = (List<String>) doc.get("members");
                    if (memberUids == null || memberUids.isEmpty()) {
                        Log.d("loadMembers", "No members found");
                        return;
                    }
                    List<MemberModel> list = new ArrayList<>();

                    for (String memberUid : memberUids) {
                        db.collection("usernames").whereEqualTo("uid", memberUid).get()
                                .addOnSuccessListener(userQuery -> {
                                    String displayName = !userQuery.isEmpty()
                                            ? userQuery.getDocuments().get(0).getId()
                                            : memberUid; //Falls back to UID if username isn't found

                                    list.add(new MemberModel(displayName, 0L, null));

                                    //This only updates the recyclerview when all members are loaded
                                    if (list.size() == memberUids.size()) {
                                        memberadapter.updateList(list);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("loadMembers", "Failed to load user: " + memberUid, e));
                    }
                })
                .addOnFailureListener(e -> Log.e("loadMembers", "Failed to load team", e));
    }
}