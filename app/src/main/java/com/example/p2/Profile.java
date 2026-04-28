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

import java.util.Locale;

public class Profile extends AppCompatActivity {

    private TextView tvBedtimeValue;

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

        if (editButton != null) {
            editButton.setOnClickListener(v -> showTimePickerDialog());
        }
        findViewById(R.id.logoutCard).setOnClickListener(v ->{
            SessionManager session = new SessionManager(this);
            session.logout();
            startActivity(new Intent(Profile.this, MainActivity.class));
        });
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
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
