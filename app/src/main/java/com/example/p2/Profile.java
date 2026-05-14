package com.example.p2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.graphics.Bitmap;
import android.widget.Toast;
import java.util.List;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class Profile extends AppCompatActivity {

    private TextView tvBedtimeValue;
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNav), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        ImageView editButton = findViewById(R.id.imageView5);
        tvBedtimeValue = findViewById(R.id.textViewBedtimeValue);
        profileImage = findViewById(R.id.imageView3);
        String username = new SessionManager(this).getUsername();
        FirebaseFirestore.getInstance().collection("usernames").document(username).get().addOnSuccessListener(doc ->{
            String bedtime = doc.getString("bedtime");
            if(bedtime != null){
                tvBedtimeValue.setText(bedtime);
            }
           SessionManager session = new SessionManager(this);
            int cachedAvatar = session.getAvatarIndex();

            if (cachedAvatar != -1){
                new Thread(()->{
                    List<Bitmap> avatars = AvatarHelper.getAvatars(this);
                    if (cachedAvatar < avatars.size()){
                        Bitmap avatar = avatars.get(cachedAvatar);
                        runOnUiThread(()-> profileImage.setImageBitmap(avatar));
                    }
                }).start();
            }else{
                Long avatarIdx = doc.getLong("avatarIndex");
                if(avatarIdx != null){
                    session.saveAvatarIndex(avatarIdx.intValue());
                    new Thread(()-> {
                        List<Bitmap> avatars = AvatarHelper.getAvatars(this);
                        if(avatarIdx >= 0 && avatarIdx < avatars.size()){
                            Bitmap avatar = avatars.get(avatarIdx.intValue());
                            runOnUiThread(()->profileImage.setImageBitmap(avatar));
                        }
                    }).start();
                }
            }
        });

        if (editButton != null) {
            editButton.setOnClickListener(v -> showTimePickerDialog());
        }

        findViewById(R.id.EditAvatar).setOnClickListener(v -> showAvatarPickerDialog());

        findViewById(R.id.PSettingsCard).setOnClickListener(v -> {
            Toast.makeText(Profile.this, "Opening Settings...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Profile.this, ProfileSettings.class));
        });

        findViewById(R.id.logoutCard).setOnClickListener(v ->{
            SessionManager session = new SessionManager(this);
            session.logout();
            startActivity(new Intent(Profile.this, MainActivity.class));
        });
        findViewById(R.id.tvNavHome).setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, ActivityHome.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavStatistics).setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, statistics.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavBlock).setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, ActivityBlock.class);
            startActivity(intent);
        });
        TextView tvUsername = findViewById(R.id.tvUsername);
        SessionManager session = new SessionManager(this);
        tvUsername.setText(session.getUsername());

        findViewById(R.id.cardGroupSettingsContainer).setOnClickListener(v -> {
            String teamCode = new SessionManager(this).getTeamCode();
            if (teamCode != null) {
                startActivity(new Intent(Profile.this, TeamPage.class));
            } else {
                startActivity(new Intent(Profile.this, JoinOrCreateTeam.class));
            }
        });
    }

    private void showAvatarPickerDialog() {
        final List<Bitmap> avatars = AvatarHelper.getAvatars(this);
        final String[] avatarLabels = new String[avatars.size()];
        for (int i = 0; i < avatars.size(); i++) {
            avatarLabels[i] = "Avatar " + (i + 1);
        }

        GridView gridView = new GridView(this);
        gridView.setNumColumns(3);
        gridView.setPadding(32, 32, 32, 32);
        gridView.setVerticalSpacing(32);
        gridView.setHorizontalSpacing(32);
        gridView.setBackgroundColor(android.graphics.Color.parseColor("#1A2B3C"));

        gridView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return avatars.size();
            }

            @Override
            public Object getItem(int position) {
                return avatars.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
                ImageView iv = new ImageView(Profile.this);
                iv.setLayoutParams(new GridView.LayoutParams(250, 250));
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setImageBitmap(avatars.get(position));
                return iv;
            }
        });

        TextView title = new TextView(this);
        title.setText("SELECT AVATAR");
        title.setPadding(0, 48, 0, 0);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(android.graphics.Color.parseColor("#FFB95F"));
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setBackgroundColor(android.graphics.Color.parseColor("#1A2B3C"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(title)
                .setView(gridView)
                .create();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            profileImage.setImageBitmap(avatars.get(position));
            
            String username = new SessionManager(Profile.this).getUsername();
            FirebaseFirestore.getInstance().collection("usernames").document(username)
                    .update("avatarIndex", position)
                    .addOnSuccessListener(aVoid -> Toast.makeText(Profile.this, "Avatar updated", Toast.LENGTH_SHORT).show());

            String label = avatarLabels[position];
            Toast.makeText(this, "Selected: " + label, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private void showTimePickerDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Bedtime");

        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 40, 50, 40);

        final NumberPicker hourPicker = new NumberPicker(this);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
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
            int hour = hourPicker.getValue();
            String minuteStr = displayedValues[minutePicker.getValue()];
            String time = String.format(Locale.getDefault(), "%02d:%s", hour, minuteStr);
            tvBedtimeValue.setText(time);
            String username = new SessionManager(Profile.this).getUsername();
            FirebaseFirestore.getInstance().collection("usernames").document(username).update("bedtime", time);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
