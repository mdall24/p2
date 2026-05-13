package com.example.p2;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;


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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNav), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
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
        btnAddApps = findViewById(R.id.btnAddApps);
        btnRemoveApps = findViewById(R.id.btnRemoveApps);
        ImageButton btnOptions = findViewById(R.id.btnTeamOptions);

        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("teams")
                .whereArrayContains("members", uid)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        teamCode = query.getDocuments().get(0).getId();
                        loadTeamTotals();
                        loadMembers();
                        loadPendingApps();
                        loadApprovedApps();

                        TeamUsageWorker.scheduleIfNeeded(this);
                        WeeklyResetWorker.scheduleIfNeeded(this);
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
            List<String> packageNames = new ArrayList<>();
            for (ApplicationInfo app : installedApps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    appNames.add(pm.getApplicationLabel(app).toString());
                    packageNames.add(app.packageName);
                }
            }

            // Sort by app name but keep package names in sync
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < appNames.size(); i++) indices.add(i);
            indices.sort((a, b) -> appNames.get(a).compareTo(appNames.get(b)));

            List<String> sortedNames = new ArrayList<>();
            List<String> sortedPackages = new ArrayList<>();
            for (int i : indices) {
                sortedNames.add(appNames.get(i));
                sortedPackages.add(packageNames.get(i));
            }

            String[] appArray = sortedNames.toArray(new String[0]);

            new AlertDialog.Builder(this).setTitle("Suggest an app").setItems(appArray, (dialog, which) -> {
                        String selectedApp = sortedNames.get(which);
                        String selectedPackage = sortedPackages.get(which);
                        suggestApp(selectedApp, selectedPackage);
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
                    String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                    db.collection("teams").document(teamCode)
                            .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(uid))
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, "You left the team", Toast.LENGTH_SHORT).show());

                    setContentView(R.layout.activity_join_or_create_team);
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
                    List<String> matchedUids = new ArrayList<>();
                    for (String memberUid : otherUids) {
                        db.collection("usernames").whereEqualTo("uid", memberUid).get()
                                .addOnSuccessListener(userQuery -> {
                                    if (!userQuery.isEmpty()) {
                                        usernames.add(userQuery.getDocuments().get(0).getId());
                                    } else {
                                        usernames.add(memberUid);
                                    }
                                    matchedUids.add(memberUid);

                                    if (usernames.size() == otherUids.size()) {
                                        String[] namesArray = usernames.toArray(new String[0]);
                                        new AlertDialog.Builder(this).setTitle("Kick a Member")
                                                .setItems(namesArray, (dialog, which) -> {
                                                    String kickedUid = matchedUids.get(which);
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

    private void suggestApp(String appName, String packageName) {
        String currentUsername = new SessionManager(this).getUsername();

        Map<String, Object> pending = new HashMap<>();
        pending.put("proposedBy", currentUsername);
        pending.put("approvals", Collections.singletonList(currentUsername));
        pending.put("rejections", new ArrayList<>());
        pending.put("packageName", packageName);

        db.collection("teams").document(teamCode)
                .update("pendingApps." + appName, pending)
                .addOnSuccessListener(a -> Log.d("suggestApp", appName + " suggested"))
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
                    if (memberUids == null || memberUids.isEmpty()) return;

                    LinearLayout container = findViewById(R.id.memberContainer);
                    container.removeAllViews();

                    for (String memberUid : memberUids) {
                        db.collection("usernames").whereEqualTo("uid", memberUid).get()
                                .addOnSuccessListener(userQuery -> {
                                    String displayName = !userQuery.isEmpty()
                                            ? userQuery.getDocuments().get(0).getId()
                                            : memberUid;

                                    View itemView = LayoutInflater.from(this)
                                            .inflate(R.layout.activity_member_adapter, container, false);

                                    TextView tvName = itemView.findViewById(R.id.tvMemberName);
                                    TextView tvTime = itemView.findViewById(R.id.tvMemberTime);
                                    TextView tvDaily = itemView.findViewById(R.id.tvMemberDaily);
                                    TextView tvWeekly = itemView.findViewById(R.id.tvMemberWeekly);
                                    TextView tvApps = itemView.findViewById(R.id.tvMemberApps);

                                    tvName.setText(displayName);
                                    tvTime.setText("Used: 0 min");
                                    tvDaily.setText("Daily: 0 min");
                                    tvWeekly.setText("Weekly: 0 min");
                                    tvApps.setText("Apps: None");

                                    container.addView(itemView);
                                });
                    }
                });
    }
    private void loadPendingApps() {
        db.collection("teams").document(teamCode).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> pendingApps = (Map<String, Object>) doc.get("pendingApps");
                    List<String> memberUids = (List<String>) doc.get("members");
                    int totalMembers = memberUids != null ? memberUids.size() : 1;

                    if (pendingApps == null || pendingApps.isEmpty()) return;
                    findViewById(R.id.tvPendingAppsTitle).setVisibility(View.VISIBLE);
                    findViewById(R.id.rvPendingApps).setVisibility(View.VISIBLE);

                    List<Map.Entry<String, Map<String, Object>>> pendingList = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : pendingApps.entrySet()) {
                        pendingList.add(new java.util.AbstractMap.SimpleEntry<>(
                                entry.getKey(),
                                (Map<String, Object>) entry.getValue()
                        ));
                    }

                    RecyclerView rvPendingApps = findViewById(R.id.rvPendingApps);
                    rvPendingApps.setLayoutManager(new LinearLayoutManager(this));
                    rvPendingApps.setAdapter(new PendingAppAdapter(pendingList, new PendingAppAdapter.PendingAppListener() {
                        @Override
                        public void onApprove(String appName) {
                            String username = new SessionManager(TeamPage.this).getUsername();
                            Map<String, Object> appData = (Map<String, Object>) pendingApps.get(appName);
                            List<String> approvals = (List<String>) appData.get("approvals");
                            List<String> rejections = (List<String>) appData.get("rejections");

                            if ((approvals != null && approvals.contains(username)) ||
                                    (rejections != null && rejections.contains(username))) {
                                Toast.makeText(TeamPage.this, "You already voted!", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            db.collection("teams").document(teamCode)
                                    .update("pendingApps." + appName + ".approvals",
                                            com.google.firebase.firestore.FieldValue.arrayUnion(username))
                                    .addOnSuccessListener(a -> checkAndFinalizePendingApp(appName));
                        }

                        @Override
                        public void onReject(String appName) {
                            String username = new SessionManager(TeamPage.this).getUsername();
                            Map<String, Object> appData = (Map<String, Object>) pendingApps.get(appName);
                            List<String> approvals = (List<String>) appData.get("approvals");
                            List<String> rejections = (List<String>) appData.get("rejections");

                            if ((approvals != null && approvals.contains(username)) ||
                                    (rejections != null && rejections.contains(username))) {
                                Toast.makeText(TeamPage.this, "You already voted!", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            db.collection("teams").document(teamCode)
                                    .update("pendingApps." + appName + ".rejections",
                                            com.google.firebase.firestore.FieldValue.arrayUnion(username))
                                    .addOnSuccessListener(a -> checkAndFinalizePendingApp(appName));
                        }
                    }, totalMembers));
                });
    }

    private void checkAndFinalizePendingApp(String appName) {
        db.collection("teams").document(teamCode).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> pendingApps = (Map<String, Object>) doc.get("pendingApps");
                    List<String> memberUids = (List<String>) doc.get("members");
                    int totalMembers = memberUids != null ? memberUids.size() : 1;

                    if (pendingApps == null) return;

                    Map<String, Object> appData = (Map<String, Object>) pendingApps.get(appName);
                    if (appData == null) return;

                    List<String> approvals = (List<String>) appData.get("approvals");
                    List<String> rejections = (List<String>) appData.get("rejections");
                    String action = (String) appData.get("action");

                    int approvalCount = approvals != null ? approvals.size() : 0;
                    int rejectionCount = rejections != null ? rejections.size() : 0;

                    if (approvalCount + rejectionCount < totalMembers) return;

                    if (approvalCount > rejectionCount) {
                        if (action != null && action.equals("remove")) {
                            db.collection("teams").document(teamCode)
                                    .update("suggestedApps", com.google.firebase.firestore.FieldValue.arrayRemove(appName));
                        } else {
                            db.collection("teams").document(teamCode)
                                    .update("suggestedApps", com.google.firebase.firestore.FieldValue.arrayUnion(appName));
                        }
                    }

                    db.collection("teams").document(teamCode)
                            .update("pendingApps." + appName, com.google.firebase.firestore.FieldValue.delete())
                            .addOnSuccessListener(a -> {
                                loadPendingApps();
                                BlockScheduler.schedule(TeamPage.this);
                            });
                });
    }
    private void loadApprovedApps() {
        db.collection("teams").document(teamCode).get()
                .addOnSuccessListener(doc -> {
                    List<String> suggestedApps = (List<String>) doc.get("suggestedApps");
                    LinearLayout container = findViewById(R.id.approvedAppsContainer);
                    container.removeAllViews();

                    if (suggestedApps == null || suggestedApps.isEmpty()) return;

                    PackageManager pm = getPackageManager();
                    for (String appName : suggestedApps) {
                        View itemView = LayoutInflater.from(this)
                                .inflate(R.layout.item_approved_app, container, false);

                        ImageView ivIcon = itemView.findViewById(R.id.ivAppIcon);
                        TextView tvName = itemView.findViewById(R.id.tvAppName);
                        TextView tvProposedBy = itemView.findViewById(R.id.tvProposedBy);

                        tvName.setText(appName);
                        tvProposedBy.setText("");

                        try {
                            ivIcon.setImageDrawable(pm.getApplicationIcon(appName));
                        } catch (PackageManager.NameNotFoundException e) {
                            ivIcon.setImageResource(android.R.drawable.sym_def_app_icon);
                        }

                        container.addView(itemView);
                    }
                });
    }
}