package com.example.p2;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeamPage extends AppCompatActivity {

    private TextView tvTeamUsed;

    private Button btnAddApps, btnRemoveApps, changeTimeBtn;

    private String teamCode;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_team_page);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
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

        tvTeamUsed = findViewById(R.id.tvTeamUsed);
        btnAddApps = findViewById(R.id.btnAddApps);
        btnRemoveApps = findViewById(R.id.btnRemoveApps);
        ImageButton btnOptions = findViewById(R.id.btnTeamOptions);
        changeTimeBtn = findViewById(R.id.changeTimeBtn);

        changeTimeBtn.setOnClickListener(v -> showTimePickerDialog());

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
                        loadPendingTimeLimit();

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
                    Intent intent = new Intent(this, ActivityAppPicker.class);
                    intent.putExtra("returnResult", true);
                    startActivityForResult(intent, 100);
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
                    new SessionManager(TeamPage.this).saveTeamCode(null);

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
                                new SessionManager(TeamPage.this).saveTeamCode(null);
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

                        Long totalRaw = (Long) doc.get("suggestedTime");
                        long total = totalRaw != null ? totalRaw : 0L;

                        Long usedRaw = (Long) doc.get("timeUsedMinutes");
                        long used = usedRaw != null ? usedRaw : 0L;

                        long left = total - used;

                        tvTeamUsed.setText("Total Used: " + formatMinutes(used));

                        TextView tvCircleText = findViewById(R.id.tvCircleText);
                        tvCircleText.setText(formatMinutes(left));

                        com.google.android.material.progressindicator.CircularProgressIndicator progress = findViewById(R.id.teamProgress);

                        int percent = total > 0 ? (int) ((double) used / total * 100) : 0;
                        progress.setProgress(100 - percent, true); //animates it (inverts the colours so it actually tracks
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

                                    db.collection("users").document(memberUid).get()
                                                    .addOnSuccessListener(userDoc -> {
                                                        long used = userDoc.getLong("timeUsedMinutes") != null ? userDoc.getLong("timeUsedMinutes") : 0;
                                                        long daily = userDoc.getLong("dailyMinutes") != null ? userDoc.getLong("dailyMinutes") : 0;
                                                        long weekly = userDoc.getLong("weeklyMinutes") != null ? userDoc.getLong("weeklyMinutes") : 0;
                                                        List<String> usedApps = (List<String>) userDoc.get("usedApps");
                                                        List<String> teamApps = (List<String>) doc.get("suggestedApps");

                                                        tvTime.setText("Used: " + formatMinutes(used));
                                                        tvDaily.setText("Daily: " + formatMinutes(daily));
                                                        tvWeekly.setText("Weekly: " + formatMinutes(weekly));

                                                        if (usedApps != null && teamApps != null){
                                                            List<String> overlap = new ArrayList<>(usedApps);
                                                            overlap.retainAll(teamApps);
                                                            tvApps.setText(overlap.isEmpty() ? "Apps: None" : "Apps: " + String.join(",", overlap));
                                                        } else {
                                                            tvApps.setText("Apps: None");
                                                        }
                                                        container.addView(itemView);
                                                    });

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
                    int majority = (totalMembers / 2) + 1;

                    if (approvalCount >= majority){
                        if (action != null && action.equals("remove")){
                            //Approves immediately as soon as majority votes
                            db.collection("teams").document(teamCode)
                                    .update("suggestedApps", com.google.firebase.firestore.FieldValue.arrayRemove(appName));
                        } else {
                            db.collection("teams").document(teamCode)
                                    .update("suggestedApps", com.google.firebase.firestore.FieldValue.arrayUnion(appName));
                        }
                        db.collection("teams").document(teamCode)
                                .update("pendingApps." + appName, com.google.firebase.firestore.FieldValue.delete())
                                .addOnSuccessListener(a -> {
                                    loadPendingApps();
                                    BlockScheduler.schedule(TeamPage.this);
                                });
                    } else if (rejectionCount >= majority){
                        //Rejects immediately once majority has decided even if not all all has voted
                        db.collection("teams").document(teamCode)
                                .update("pendingApps." + appName, com.google.firebase.firestore.FieldValue.delete())
                                .addOnSuccessListener(a -> loadPendingApps());
                    }
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null){
            List<String> packages = data.getStringArrayListExtra("selectedPackages");
            PackageManager pm = getPackageManager();
            for (String packageName : packages) {
                try {
                    String appName = pm.getApplicationLabel(
                            pm.getApplicationInfo(packageName, 0)).toString();
                    suggestApp(appName, packageName);
                } catch (PackageManager.NameNotFoundException e){
                    Log.e("TeamPage", "App not found; " + packageName);
                }
            }
        }
    }

    private void suggestTimeLimit(long newLimitMinutes){
        String currentUsername = new SessionManager(this).getUsername();

        Map<String, Object> proposal = new HashMap<>();
        proposal.put("proposedBy", currentUsername);
        proposal.put("approvals", Collections.singletonList(currentUsername));
        proposal.put("rejections", new ArrayList<>());
        proposal.put("newLimit", newLimitMinutes);

        db.collection("teams").document(teamCode)
                .update("pendingTimeLimit", proposal)
                .addOnSuccessListener(a -> Toast.makeText(this, "Time Limit change suggested!", Toast.LENGTH_SHORT). show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to suggest time", Toast.LENGTH_SHORT).show());
    }

    private void loadPendingTimeLimit() {
        db.collection("teams").document(teamCode).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> proposal = (Map<String, Object>) doc.get("pendingTimeLimit");
                    if (proposal == null) return;

                    List<String> memberUids = (List<String>) doc.get("members");
                    int totalMembers = memberUids != null ? memberUids.size() : 1;

                    String proposedBy = (String) proposal.get("proposedBy");
                    long newLimit = (long) proposal.get("newLimit");
                    List<String> approvals = (List<String>) proposal.get("approvals");
                    List<String> rejections = (List<String>) proposal.get("rejections");
                    String currentUsername = new SessionManager(this).getUsername();

                    //Show a banner/dialog to vote if not yet voted
                    boolean alreadyVoted = (approvals != null && approvals.contains(currentUsername))
                            || (rejections != null && rejections.contains(currentUsername));

                    if (!alreadyVoted){
                        new AlertDialog.Builder(this)
                                .setTitle("Pending: Change Time Limit")
                                .setMessage(proposedBy + " suggests changing the team time limit to " + formatMinutes(newLimit) + ". Do you approve?")
                                .setPositiveButton("Approve", (dialog, which) -> {
                                    db.collection("teams").document(teamCode)
                                            .update("pendingTimeLimit.approvals",
                                                    com.google.firebase.firestore.FieldValue.arrayUnion(currentUsername))
                                            .addOnSuccessListener(a -> checkAndFinalizeTimeLimit());
                                })
                                .setNegativeButton("Reject", (dialog, which) -> {
                                    db.collection("teams").document(teamCode)
                                            .update("pendingTimeLimit.rejections",
                                                    com.google.firebase.firestore.FieldValue.arrayUnion(currentUsername))
                                            .addOnSuccessListener(a -> checkAndFinalizeTimeLimit());
                                })
                                .setCancelable(false)
                                .show();
                    }
                });
    }
    private void checkAndFinalizeTimeLimit() {
        db.collection("teams").document(teamCode).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> proposal = (Map<String, Object>) doc.get("pendingTimeLimit");
                    if (proposal == null) return;

                    List<String> memberUids = (List<String>) doc.get("members");
                    int totalMembers = memberUids != null ? memberUids.size() : 1;

                    List<String> approvals = (List<String>) proposal.get("approvals");
                    List<String> rejections = (List<String>) proposal.get("rejections");
                    long newLimit = (long) proposal.get("newLimit");

                    int approvalCount = approvals != null ? approvals.size() : 0;
                    int rejectionCount = rejections != null ? rejections.size() : 0;

                    if (approvalCount + rejectionCount < totalMembers) return;

                    if (approvalCount > rejectionCount) {
                        db.collection("teams").document(teamCode)
                                .update("suggestedTime", newLimit)
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(this, "Time limit updated to " + formatMinutes(newLimit)+ "!", Toast.LENGTH_SHORT).show();
                                    loadTeamTotals();
                                });
                    } else {
                        Toast.makeText(this, "Time limit change rejected by the team.", Toast.LENGTH_SHORT).show();
                    }

                    //Clears the proposal either way
                    db.collection("teams").document(teamCode)
                            .update("pendingTimeLimit", com.google.firebase.firestore.FieldValue.delete());
                });
    }
    private void showTimePickerDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Suggest Time Limit");

        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 40, 50, 40);

        final NumberPicker hourPicker = new NumberPicker(this);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(200);
        hourPicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));

        final NumberPicker minutePicker = new NumberPicker(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(5);
        final String[] displayedValues = {"00", "10", "20", "30", "40", "50"};
        minutePicker.setDisplayedValues(displayedValues);


        // Add a colon text view between pickers
        TextView colon = new TextView(this);
        colon.setText(" : ");
        colon.setTextSize(20);

        layout.addView(hourPicker);
        layout.addView(colon);
        layout.addView(minutePicker);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int hours = hourPicker.getValue();
            int minutes = Integer.parseInt(displayedValues[minutePicker.getValue()]);
            long totalMinutes = (hours * 60L) + minutes;
            suggestTimeLimit(totalMinutes);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private String formatMinutes(long minutes){
        if (minutes < 60) return minutes + " min";
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (mins == 0) return hours + "h";
        return hours + "h" + mins + "m";
    }
}