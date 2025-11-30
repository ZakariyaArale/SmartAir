package com.example.smartairsetup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class EmergencyActivity extends AppCompatActivity {

    private String parentUid;
    private boolean cantSpeakFullSentences;
    private boolean chestRetractions;
    private boolean blueLipsNails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        Button callButton = findViewById(R.id.buttonCallEmergency);
        Button backButton = findViewById(R.id.buttonBack);
        Button nextButton = findViewById(R.id.buttonNext);

        Intent incomingIntent = getIntent();
        parentUid = incomingIntent.getStringExtra("PARENT_UID");
        cantSpeakFullSentences = incomingIntent.getBooleanExtra("cantSpeakFullSentences", false);
        chestRetractions = incomingIntent.getBooleanExtra("chestRetractions", false);
        blueLipsNails = incomingIntent.getBooleanExtra("blueLipsNails", false);

        callButton.setOnClickListener(v -> {
            String emergencyNumber = "911";
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + emergencyNumber));
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RedFlagsActivity.class);
            intent.putExtra("PARENT_UID", parentUid);
            startActivity(intent);
        });
        
        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmergencySelectorActivity.class);
            intent.putExtra("PARENT_UID", parentUid);
            intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            intent.putExtra("chestRetractions", chestRetractions);
            intent.putExtra("blueLipsNails", blueLipsNails);
            startActivity(intent);
        });
    }
}
