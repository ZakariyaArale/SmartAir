package com.example.smartairsetup;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button buttonNewUser;
    private Button buttonExistingUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonNewUser = findViewById(R.id.buttonNewUser);
        buttonExistingUser = findViewById(R.id.buttonExistingUser);

        buttonNewUser.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OnboardingActivity.class);
            startActivity(intent);
        });

        buttonExistingUser.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        Button buttonDebugAddChild = findViewById(R.id.buttonDebugAddChild);
        buttonDebugAddChild.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddChildActivity.class);
            startActivity(intent);
        });
    }
}