package com.example.p2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ChangePassword extends AppCompatActivity {

    private EditText newPassword, confirmNewPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnBackHeader).setOnClickListener(v -> finish());

        newPassword = findViewById(R.id.NewPassword);
        confirmNewPassword = findViewById(R.id.ConfirmNewPassword);

        findViewById(R.id.cardUpdatePassword).setOnClickListener(v -> updatePassword());
    }

    private void updatePassword() {
        String pass = newPassword.getText().toString().trim();
        String confirmPass = confirmNewPassword.getText().toString().trim();

        if (pass.isEmpty()) {
            newPassword.setError("Password is required");
            newPassword.requestFocus();
            return;
        }

        if (pass.length() < 6) {
            newPassword.setError("Minimum length is 6 characters");
            newPassword.requestFocus();
            return;
        }

        if (!pass.equals(confirmPass)) {
            confirmNewPassword.setError("Passwords do not match");
            confirmNewPassword.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String username = new SessionManager(this).getUsername();
                    if (username != null && !username.isEmpty()) {
                        FirebaseFirestore.getInstance().collection("usernames").document(username)
                                .update("password", pass)
                                .addOnCompleteListener(dbTask -> {
                                    Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                    new SessionManager(this).logout();
                                    Intent intent = new Intent(this, Login.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                });
                    } else {
                        Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        Intent intent = new Intent(this, Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(this, "Failed to update password. Try logging in again.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
