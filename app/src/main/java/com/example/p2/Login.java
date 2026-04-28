package com.example.p2;

import android.content.Intent;
import android.os.Bundle;
import android.se.omapi.Session;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

private Button login;
private EditText pass_word, user_name;
private FirebaseAuth mAuth;
private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        login = findViewById(R.id.Login);
        user_name = findViewById(R.id.Username);
        pass_word = findViewById(R.id.Password);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        login.setOnClickListener(V -> loginUser());
        }
    private void loginUser(){

            String username = user_name.getText().toString().trim();
            String password = pass_word.getText().toString().trim();


            if (username.isEmpty()) {
                user_name.setError("Please enter username");
                user_name.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                pass_word.setError("Please enter password");
                pass_word.requestFocus();
                return;
            }
            db.collection("usernames").document(username).get().addOnSuccessListener(doc -> {
                if(!doc.exists()){
                    user_name.setError("Username not found");
                    user_name.requestFocus();
                    return;
                }
                String email = doc.getString("email");
                if (email == null) {
                    Toast.makeText(Login.this, "Email not found for this username", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Toast.makeText(Login.this, "Login successful", Toast.LENGTH_SHORT).show();
                        SessionManager session = new SessionManager(Login.this);
                        session.saveLoginSession(username);
                        startActivity(new Intent(Login.this, ActivityHome.class));
                        finish();
                    }
                    else
                    {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(Login.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .addOnFailureListener(e -> Toast.makeText(Login.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }