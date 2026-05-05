package com.example.p2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView btnBackHeader = findViewById(R.id.btnBackHeader);
        btnBackHeader.setOnClickListener(v -> finish());

        Button btnBackToProfile = findViewById(R.id.btnBackToProfile);
        btnBackToProfile.setOnClickListener(v -> finish());

        findViewById(R.id.cardDeleteAccount).setOnClickListener(v -> deleteAccount());
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String username = new SessionManager(this).getUsername();
            if (username != null && !username.isEmpty()) {
                FirebaseFirestore.getInstance().collection("usernames").document(username).delete()
                        .addOnCompleteListener(dbTask -> {
                            user.delete().addOnCompleteListener(authTask -> {
                                if (authTask.isSuccessful()) {
                                    Toast.makeText(this, "Account and data deleted", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(ProfileSettings.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(this, "Failed to delete account. You may need to log-in again.", Toast.LENGTH_LONG).show();
                                }
                            });
                        });
            } else {
                user.delete().addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        Toast.makeText(this, "Account Deleted", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ProfileSettings.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Failed to delete account. You may need to log-in again.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
        }
    }
}
