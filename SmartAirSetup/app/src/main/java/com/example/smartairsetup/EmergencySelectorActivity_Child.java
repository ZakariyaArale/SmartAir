package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmergencySelectorActivity_Child extends AppCompatActivity {

    private Button saveButton, backButton, nextButton, recordMedicationButton, chooseChildButton;
    private EditText followUpInput;

    private FirebaseFirestore db;
    private String childId;
    private String parentUid;

    // Red flags passed from EmergencyActivity_Child
    private boolean cantSpeakFullSentences;
    private boolean chestRetractions;
    private boolean blueLipsNails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_select_child);

        // --- Initialize UI ---
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);
        recordMedicationButton = findViewById(R.id.recordMedicationButton);
        chooseChildButton = findViewById(R.id.chooseChildButton);
        followUpInput = findViewById(R.id.followUpInput);

        db = FirebaseFirestore.getInstance();

        // --- Retrieve parent UID, child ID, and red flags ---
        Intent intent = getIntent();
        parentUid = intent.getStringExtra("PARENT_UID");
        childId = intent.getStringExtra("CHILD_ID");
        cantSpeakFullSentences = intent.getBooleanExtra("cantSpeakFullSentences", false);
        chestRetractions = intent.getBooleanExtra("chestRetractions", false);
        blueLipsNails = intent.getBooleanExtra("blueLipsNails", false);

        if (parentUid == null || childId == null) {
            Toast.makeText(this, "Missing parent or child ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // --- Load child name and display on chooseChildButton only ---
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String childName = doc.getString("name");
                        if (childName != null && !childName.isEmpty()) {
                            chooseChildButton.setText(childName);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("EmergencySelectorChild", "Failed to load child name", e));

        // --- Save button → logs triage ---
        saveButton.setOnClickListener(v -> saveTriageLog());

        // --- Record Medication → goes to RecordMedicationTriage ---
        recordMedicationButton.setOnClickListener(v -> {
            Intent recordIntent = new Intent(EmergencySelectorActivity_Child.this, RecordMedicationTriage.class);
            recordIntent.putExtra("PARENT_UID", parentUid);
            recordIntent.putExtra("CHILD_ID", childId);
            recordIntent.putExtra("returnClass", EmergencySelectorActivity_Child.this.getClass().getName());
            recordIntent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            recordIntent.putExtra("chestRetractions", chestRetractions);
            recordIntent.putExtra("blueLipsNails", blueLipsNails);
            startActivity(recordIntent);
        });

        // --- Back button → returns to EmergencyActivity_Child ---
        backButton.setOnClickListener(v -> {
            Intent backIntent = new Intent(EmergencySelectorActivity_Child.this, EmergencyActivity_Child.class);
            backIntent.putExtra("PARENT_UID", parentUid);
            backIntent.putExtra("CHILD_ID", childId);
            backIntent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            backIntent.putExtra("chestRetractions", chestRetractions);
            backIntent.putExtra("blueLipsNails", blueLipsNails);
            startActivity(backIntent);
            finish();
        });

        // --- Next button → goes to ChildHomeActivity ---
        nextButton.setOnClickListener(v -> {
            Intent intentNext = new Intent(EmergencySelectorActivity_Child.this, ChildHomeActivity.class);
            intentNext.putExtra("PARENT_UID", parentUid);
            intentNext.putExtra("CHILD_ID", childId);
            startActivity(intentNext);
        });
    }

    private void saveTriageLog() {
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Child ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = followUpInput.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Enter follow-up notes", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateId = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("triage")
                .document("logs")
                .collection("entries")
                .document(dateId)
                .set(new HashMap<String, Object>() {{
                    put("triage-zone", "Emergency");
                    put("message-triage", message);
                    put("timestamp", System.currentTimeMillis());
                    put("cantSpeakFullSentences", cantSpeakFullSentences);
                    put("chestRetractions", chestRetractions);
                    put("blueLipsNails", blueLipsNails);
                }})
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Emergency triage logged", Toast.LENGTH_SHORT).show();
                    followUpInput.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
