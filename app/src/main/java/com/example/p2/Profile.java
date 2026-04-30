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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
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
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
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
        });

        if (editButton != null) {
            editButton.setOnClickListener(v -> showTimePickerDialog());
        }

        findViewById(R.id.EditAvatar).setOnClickListener(v -> showAvatarPickerDialog());

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
    }

    private void showAvatarPickerDialog() {
        final String[] avatarLabels = {"Avatar 1", "Avatar 2", "Avatar 3", "Avatar 4", "Avatar 5", "Avatar 6"};
        final int[] colors = {
            0, // Original
            android.graphics.Color.parseColor("#FFD700"), // Gold
            android.graphics.Color.parseColor("#C0C0C0"), // Silver
            android.graphics.Color.parseColor("#CD7F32"), // Bronze
            android.graphics.Color.parseColor("#2196F3"), // Blue
            android.graphics.Color.parseColor("#4CAF50")  // Green
        };

        GridView gridView = new GridView(this);
        gridView.setNumColumns(3);
        gridView.setPadding(32, 32, 32, 32);
        gridView.setVerticalSpacing(32);
        gridView.setHorizontalSpacing(32);
        gridView.setBackgroundColor(android.graphics.Color.parseColor("#1A2B3C"));

        gridView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return avatarLabels.length;
            }

            @Override
            public Object getItem(int position) {
                return avatarLabels[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
                ImageView iv = new ImageView(Profile.this);
                iv.setLayoutParams(new GridView.LayoutParams(250, 250));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageResource(R.drawable.default_avatar);
                
                if (colors[position] != 0) {
                    iv.setColorFilter(colors[position], android.graphics.PorterDuff.Mode.MULTIPLY);
                } else {
                    iv.clearColorFilter();
                }
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
            profileImage.setImageResource(R.drawable.default_avatar);
            if (colors[position] != 0) {
                profileImage.setColorFilter(colors[position], android.graphics.PorterDuff.Mode.MULTIPLY);
            } else {
                profileImage.clearColorFilter();
            }
            
            String label = avatarLabels[position];
            android.widget.Toast.makeText(this, "Selected: " + label, android.widget.Toast.LENGTH_SHORT).show();
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
        hourPicker.setMaxValue(24);
        hourPicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));

        final NumberPicker minutePicker = new NumberPicker(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(5);
        final String[] displayedValues = {"00", "10", "20", "30", "40", "50"};
        minutePicker.setDisplayedValues(displayedValues);

        // If 24 is selected, minute can only be 00
        hourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (newVal == 24) {
                minutePicker.setValue(0);
                minutePicker.setEnabled(false);
            } else {
                minutePicker.setEnabled(true);
            }
        });

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
