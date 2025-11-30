package com.example.smartairsetup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmergencyActivity_Child extends AppCompatActivity {

    private static final String TAG = "EmergencyActivity_Child";

    private String parentUid;
    private String childId;
    private boolean cantSpeakFullSentences;
    private boolean chestRetractions;
    private boolean blueLipsNails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_child);

        Button callButton = findViewById(R.id.buttonCallEmergency);
        Button backButton = findViewById(R.id.buttonBack);
        Button nextButton = findViewById(R.id.buttonNext);

        // Retrieve data from intent
        Intent intent = getIntent();
        parentUid = intent.getStringExtra("PARENT_UID");
        childId = intent.getStringExtra("CHILD_ID"); // <-- consistent key
        cantSpeakFullSentences = intent.getBooleanExtra("cantSpeakFullSentences", false);
        chestRetractions = intent.getBooleanExtra("chestRetractions", false);
        blueLipsNails = intent.getBooleanExtra("blueLipsNails", false);

        Log.d(TAG, "onCreate: parentUid=" + parentUid + ", childId=" + childId);

        if (parentUid == null || parentUid.isEmpty() || childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Missing parent or child ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Call emergency service
        callButton.setOnClickListener(v -> {
            String emergencyNumber = "911";
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + emergencyNumber));
            startActivity(callIntent);
        });

        // Back button: go to RedFlagsActivity_Child
        backButton.setOnClickListener(v -> {
            Intent backIntent = new Intent(this, RedFlagsActivity_Child.class);
            backIntent.putExtra("PARENT_UID", parentUid);
            backIntent.putExtra("CHILD_ID", childId);
            startActivity(backIntent);
            finish();
        });

        // Next button: go to EmergencySelectorActivity_Child
        nextButton.setOnClickListener(v -> {
            Intent nextIntent = new Intent(this, EmergencySelectorActivity_Child.class);
            nextIntent.putExtra("PARENT_UID", parentUid);
            nextIntent.putExtra("CHILD_ID", childId); // <-- consistent key
            nextIntent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            nextIntent.putExtra("chestRetractions", chestRetractions);
            nextIntent.putExtra("blueLipsNails", blueLipsNails);
            startActivity(nextIntent);
        });
    }
}
