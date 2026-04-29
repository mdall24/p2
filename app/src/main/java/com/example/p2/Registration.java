package com.example.p2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Registration extends AppCompatActivity {
    private Button sign_up;
    private TextView cancel;
    private EditText user_name, pass_word, e_mail, confirm_password;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
            user_name = findViewById(R.id.Username);
            pass_word = findViewById(R.id.Password);
            e_mail = findViewById(R.id.EmailAddress);
            sign_up = findViewById(R.id.SignUp);
            confirm_password = findViewById(R.id.ConfirmPassword);
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            cancel = findViewById(R.id.cancel);
            cancel.setOnClickListener(v -> finish());

            sign_up.setOnClickListener(v -> registerUser());

            sign_up.setOnClickListener(v -> {
                    String email = e_mail.getText().toString().trim();
                    String username = user_name.getText().toString().trim();
                    String password = pass_word.getText().toString().trim();
                    String cpassword = confirm_password.getText().toString().trim();
                    if(email.isEmpty())
                    {
                        e_mail.setError("Email is empty");
                        e_mail.requestFocus();
                        return;
                    }
                    if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
                    {
                        e_mail.setError("Enter a valid email address");
                        e_mail.requestFocus();
                        return;

                    }
                    if(username.isEmpty())
                    {
                        user_name.setError("Please enter a username");
                        user_name.requestFocus();
                        return;
                    }
                    if(password.isEmpty())
                    {
                        pass_word.setError("Please enter a password");
                        pass_word.requestFocus();
                        return;
                    }
                    if(password.length()<6)
                    {
                        pass_word.setError("Password needs minimum 6 characters");
                        pass_word.requestFocus();
                        return;
                    }
                    if(cpassword.isEmpty())
                    {
                        confirm_password.setError("Please confirm password");
                        confirm_password.requestFocus();
                        return;
                    }
                    if(!cpassword.matches(password))
                    {
                        confirm_password.setError("Passwords doesn't match");
                        confirm_password.requestFocus();
                        return;
                    }
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                        if(task.isSuccessful() && task.getResult().getUser() != null)
                        {
                            String uid = task.getResult().getUser().getUid();
                            FirebaseFirestore.getInstance().collection("users").document(uid).set(new User(username, email));
                            FirebaseFirestore.getInstance().collection("usernames").document(username).set(new User(username, email));

                            Toast.makeText(Registration.this,"You are successfully registered", Toast.LENGTH_SHORT).show();
                            SessionManager session = new SessionManager(Registration.this);
                            session.saveLoginSession(username);
                            startActivity(new Intent(Registration.this, ActivityHome.class));
                        }
                        else
                        {
                            String message = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(Registration.this,"Error: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
            });
    }

    private void registerUser() {
        String email = e_mail.getText().toString().trim();
        String username = user_name.getText().toString().trim();
        String password = pass_word.getText().toString().trim();
        String cpassword = confirm_password.getText().toString().trim();

        if(email.isEmpty()) {
            e_mail.setError("Email is empty");
            e_mail.requestFocus();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            e_mail.setError("Enter a valid email address");
            e_mail.requestFocus();
            return;
        }
        if(username.isEmpty()) {
            user_name.setError("Please enter a username");
            user_name.requestFocus();
            return;
        }
        if(password.isEmpty()) {
            pass_word.setError("Please enter a password");
            pass_word.requestFocus();
            return;
        }
        if(password.length()<6) {
            pass_word.setError("Password needs minimum 6 characters");
            pass_word.requestFocus();
            return;
        }
        if(cpassword.isEmpty()) {
            confirm_password.setError("Please confirm password");
            confirm_password.requestFocus();
            return;
        }
        if(!cpassword.equals(password)) {
            confirm_password.setError("Passwords don't match");
            confirm_password.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();

                // Store username mapping for login
                Map<String, Object> usernameMap = new HashMap<>();
                usernameMap.put("email", email);
                db.collection("usernames").document(username).set(usernameMap);

                // Store user profile
                db.collection("users").document(uid).set(new User(username, email))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Registration.this,"You are successfully registered", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Registration.this, ActivityHome.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Registration.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            } else {
                String message = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                Toast.makeText(Registration.this,"Error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
