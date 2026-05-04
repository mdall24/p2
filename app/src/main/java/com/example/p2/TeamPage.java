package com.example.p2;

import android.app.AlertDialog;
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
import androidx.core.content.PackageManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        tvTeamTotal = findViewById(R.id.tvTeamTotal);
        tvTeamUsed = findViewById(R.id.tvTeamUsed);
        rvMembers = findViewById(R.id.rvMembers);

        teamCode = new SessionManager(this).getTeamCode();

        memberadapter = new MemberAdapter(new ArrayList<>());
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(memberadapter);

        btnAddApps = findViewById(R.id.btnAddApps);
        btnRemoveApps = findViewById(R.id.btnRemoveApps);

        ImageButton btnOptions = findViewById(R.id.btnTeamOptions);

        btnOptions.setOnClickListener(v -> {
            View bottomSheetView = getLayoutInflater().inflate(R.layout.team_options, null);
            BottomSheetDialog bottomSheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
            bottomSheet.setContentView(bottomSheetView);

            //This will check if someone is leader and show only leader options
            boolean isLeader = true;

            TextView btnKickMember = bottomSheetView.findViewById(R.id.btnKickMember);
            TextView btnDisbandTeam = bottomSheetView.findViewById(R.id.btnDisbandTeam);
            View dividerDisband = bottomSheetView.findViewById(R.id.dividerDisband);
            TextView btnLeaveTeam = bottomSheetView.findViewById(R.id.btnLeaveTeam);

            if (isLeader) {
                btnKickMember.setVisibility(View.VISIBLE);
                btnDisbandTeam.setVisibility(View.VISIBLE);
                dividerDisband.setVisibility(View.VISIBLE);
            }

            btnLeaveTeam.setOnClickListener(view -> {
                bottomSheet.dismiss();
                showLeaveConfirmation();
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
            //Gets all installed apps
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            //Builds a readable name list and Filters out system apps
            List<String> appNames = new ArrayList<>();
            for (ApplicationInfo app : installedApps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    appNames.add(pm.getApplicationLabel(app).toString());
                }
            }
            //Sorts it alphabetically
            Collections.sort(appNames);

            String[] appArray = appNames.toArray(new String[0]);

            new AlertDialog.Builder(this).setTitle("Suggest an app").setItems(appArray, (dialog, which) -> {
                        String selectedApps = appArray[which];
                        suggestApp(selectedApps);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).show();
        });

        btnRemoveApps.setOnClickListener(v -> {
            //Gets the current suggested apps list from Firestore
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

        loadTeamTotals();
        loadMembers();
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
        db.collection("teams").document(teamCode).collection("members")
                .get()
                .addOnSuccessListener(querySnapshot ->{
                    List<String> memberNames = new ArrayList<>();
                    String currentUsername = new SessionManager(this).getUsername();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()){
                        String uid = doc.getId();
                        if(!uid.equals(currentUsername)){
                            memberNames.add(uid);
                        }
                    }

                    String[] namesArray = memberNames.toArray(new String[0]);

                    new AlertDialog.Builder(this).setTitle("Kick a Member")
                            .setItems(namesArray, (dialog, which) -> {
                                String kickedName = memberNames.get(which);
                                new AlertDialog.Builder(this)
                                        .setTitle("Kick " + kickedName + "?")
                                        .setMessage("Are you sure you want to kick " + kickedName + " from the team?")
                                        .setPositiveButton("Kick", (d, w) -> kickMember(kickedName))
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            })
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load members", Toast.LENGTH_SHORT).show());
    }
    private void kickMember(String username){
        db.collection("teams").document(teamCode)
                .collection("members").document(username)
                .delete()
                .addOnSuccessListener(unused ->
                    Toast.makeText(this, username + " has been kicked", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to kick member", Toast.LENGTH_SHORT).show());
    }

    private void showDisbandConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Disband Team?")
                .setMessage("This will permanently disband team for everyone. Are you sure?")
                .setPositiveButton("Disband", (dialog, which) -> {
                    // Handles disband logic
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void suggestApp(String appName){
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

    private void suggestRemoveApp(String appName){
        String currentUsername = new SessionManager(this).getUsername();

        Map<String, Object> pending = new HashMap<>();
        pending.put("proposedBy", currentUsername);
        pending.put("approvals", Collections.singletonList(currentUsername));
        pending.put("rejections", new ArrayList<>());
        pending.put("action", "remove"); //marks as removal suggestion

        db.collection("teams").document("teamCode")
                .update(("pendingApps.") + appName, pending)
                .addOnSuccessListener(a -> Log.d("suggestRemoveApp", appName + " removal suggested"))
                .addOnFailureListener(e -> Log.e("suggestRemoveApp", "Failed", e));
    }
    private void loadTeamTotals(){
        db.collection("teams").document(teamCode).get()
                .addOnSuccessListener(doc ->{
            if (doc.exists()){

                String teamName = doc.getString("teamName");
                TextView tvTeamName = findViewById(R.id.TeamPage);
                tvTeamName.setText(teamName != null ? teamName : "No Name");

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
        db.collection("teams").document(teamCode).collection("members")
                .get().addOnSuccessListener(query ->{
            List<MemberModel> list = new ArrayList<>();

            for(QueryDocumentSnapshot doc : query) {
            String username = doc.getId();

            //Handles a potential null from Firestore
            Long timeUsedRaw = doc.getLong("timeUsed");
            long timeUsed = timeUsedRaw != null ? timeUsedRaw : 0L;

            @SuppressWarnings("unchecked") //Suppresses the compiler warnings
            Map<String, Map<String, Long>> apps = (Map<String, Map<String, Long>>) doc.get("appsUsed");

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