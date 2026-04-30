package com.example.p2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private Button btnCreateAcc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(MainActivity.this, ActivityHome.class));
                finish();
            } else {
                session.logout();
                startActivity(new Intent(MainActivity.this, Login.class));
                finish();
            }
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button loginBtn = findViewById(R.id.Login);
        loginBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Login.class));
        });
        
        btnCreateAcc = findViewById(R.id.createAcc);
        btnCreateAcc.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Registration.class);
            startActivity(intent);
        });

    }
}
