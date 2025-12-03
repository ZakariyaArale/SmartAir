package com.example.smartairsetup.triage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class OptionalDataActivity_Child extends AppCompatActivity {

    private Button chooseChildButton;
    private EditText followUpInput;

    private Intent intent;
    private FirebaseFirestore db;
    private String childId;
    private String parentUid;

    // Red flags from previous screen
    private boolean cantSpeakFullSentences;
    private boolean chestRetractions;
    private boolean blueLipsNails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optional_data_child);

        Button saveButton = findViewById(R.id.saveButton);
        Button backButton = findViewById(R.id.backButton);
        Button nextButton = findViewById(R.id.nextButton);
        followUpInput = findViewById(R.id.followUpInput);
        chooseChildButton = findViewById(R.id.chooseChildButton);
        Button recordMedicationButton = findViewById(R.id.recordMedicationButton);

        db = FirebaseFirestore.getInstance();

        // Receive parentUid, childId, red flags
        intent = getIntent();
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

        // Fetch child name to show on button
        fetchChildName();

        saveButton.setOnClickListener(v -> saveTriageLog());
        recordMedicationButton.setOnClickListener(v -> {
            intent = new Intent(OptionalDataActivity_Child.this, RecordMedicationTriage.class);
            intent.putExtra("PARENT_UID", parentUid);
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            intent.putExtra("chestRetractions", chestRetractions);
            intent.putExtra("blueLipsNails", blueLipsNails);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            Intent backIntent = new Intent(OptionalDataActivity_Child.this, EmergencyActivity_Child.class);
            backIntent.putExtra("PARENT_UID", parentUid);
            backIntent.putExtra("CHILD_ID", childId);
            backIntent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            backIntent.putExtra("chestRetractions", chestRetractions);
            backIntent.putExtra("blueLipsNails", blueLipsNails);
            startActivity(backIntent);
            finish();
        });

        nextButton.setOnClickListener(v -> fetchZoneAndLaunch());
    }

    private void fetchChildName() {
        DocumentReference childRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId);

        childRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String childName = doc.getString("name");
                if (childName != null && !childName.isEmpty()) {
                    chooseChildButton.setText(childName);
                } else {
                    chooseChildButton.setText("Unknown Child");
                }
            } else {
                chooseChildButton.setText("Unknown Child");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load child name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            chooseChildButton.setText("Unknown Child");
        });
    }

    private void saveTriageLog() {
        String message = followUpInput.getText().toString().trim();
        String dateId = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DocumentReference logRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("triage")
                .document("logs")
                .collection("entries")
                .document(dateId);

        Map<String, Object> data = new HashMap<>();
        if (!message.isEmpty()) data.put("message-triage", message);
        data.put("timestamp", System.currentTimeMillis());
        data.put("cantSpeakFullSentences", cantSpeakFullSentences);
        data.put("chestRetractions", chestRetractions);
        data.put("blueLipsNails", blueLipsNails);

        logRef.set(data)
                .addOnSuccessListener(a -> Toast.makeText(this, "Triage log saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save triage log: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void fetchZoneAndLaunch() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference triageRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("triage")
                .document("logs")
                .collection("entries")
                .document(today);

        triageRef.get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getString("zone") != null) {
                launchZoneActivity(Objects.requireNonNull(doc.getString("zone")));
            } else {
                DocumentReference latestPEFRef = db.collection("users")
                        .document(parentUid)
                        .collection("children")
                        .document(childId)
                        .collection("PEF")
                        .document("latest");

                latestPEFRef.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && snapshot.getString("zone") != null) {
                        launchZoneActivity(Objects.requireNonNull(snapshot.getString("zone")));
                    } else {
                        Toast.makeText(this, "Please record PEF", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch PEF: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to fetch triage: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void launchZoneActivity(String zone) {
        Intent intent;
        switch (zone.toUpperCase()) {
            case "GREEN":
                intent = new Intent(this, GreenCardActivity.class);
                break;
            case "YELLOW":
                intent = new Intent(this, YellowCardActivity.class);
                break;
            case "RED":
                intent = new Intent(this, RedCardActivity.class);
                break;
            default:
                Toast.makeText(this, "Invalid zone: " + zone, Toast.LENGTH_SHORT).show();
                return;
        }

        intent.putExtra("PARENT_UID", parentUid);
        intent.putExtra("CHILD_ID", childId);
        intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
        intent.putExtra("chestRetractions", chestRetractions);
        intent.putExtra("blueLipsNails", blueLipsNails);
        intent.putExtra(GreenCardActivity.EXTRA_IS_CHILD, 1);

        startActivity(intent);
    }
}
