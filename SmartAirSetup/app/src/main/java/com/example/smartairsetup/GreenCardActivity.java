package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class GreenCardActivity extends AppCompatActivity {
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_green_card); // Make sure this matches your XML filename


        Button backButton = findViewById(R.id.backButton);
        Button nextButton = findViewById(R.id.nextButton);

        backButton.setOnClickListener(v -> {
            intent = new Intent(this, OptionalDataActivity.class);
            startActivity(intent);
        });

        nextButton.setOnClickListener(v -> {
            finish(); //TODO: FIX TO CORRECT SCREEN ROHAT
        });

    }
}