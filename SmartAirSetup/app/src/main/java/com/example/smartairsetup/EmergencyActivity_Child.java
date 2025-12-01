package com.example.smartairsetup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmergencyActivity_Child extends AppCompatActivity {

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_child); // Make sure this matches your XML filename

        Button callButton = findViewById(R.id.buttonCallEmergency);
        Button backButton = findViewById(R.id.buttonBack);
        Button nextButton = findViewById(R.id.buttonNext);

        // Call emergency service when pressed
        callButton.setOnClickListener(v -> {
            // Emergency number, default 911
            String emergencyNumber = "911";
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + emergencyNumber));
            startActivity(intent);
        });

        // Back button
        backButton.setOnClickListener(v -> {
            intent = new Intent(this, RedFlagsActivity.class);
            startActivity(intent);
        });

        // Next button: handle navigation if needed
        nextButton.setOnClickListener(v ->
                Toast.makeText(this, "Next screen not implemented", Toast.LENGTH_SHORT).show()
        );
    }
}