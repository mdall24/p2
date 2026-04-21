package com.example.p2;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class Registration extends AppCompatActivity {
    private Button sign_up;
    private Button cancel;
    private EditText user_name, pass_word, e_mail;
    FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
            user_name = findViewById(R.id.Username);
            pass_word = findViewById(R.id.Password);
            e_mail = findViewById(R.id.EmailAddress);
            sign_up = findViewById(R.id.SignUp);
            mAuth = FirebaseAuth.getInstance();
            sign_up.setOnClickListener(new View.OnClickListener(){
                @Override
                        public void onClick(View v){
                    String email = e_mail.getText().toString().trim();
                    String username = user_name.getText().toString().trim();
                    String password = pass_word.getText().toString().trim();
                    if(email.isEmpty())
                    {
                        e_mail.setError("Email is empty");
                        e_mail.requestFocus();
                        return;
                    }
                    if(!Patterns.EMAIL_ADDRESS.matcher(password).matches())
                    {
                        e_mail.setError("Enter a valid email address");

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

                }
            });
        });
    }
}