package com.example.smartairsetup.triage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.example.smartairsetup.pef.ChildDiaglog;
import com.example.smartairsetup.pef.FireBaseProcessChild;
import com.example.smartairsetup.pef.ProcessChildren;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OptionalDataActivity extends AppCompatActivity {

    private Button chooseChildButton;
    private EditText followUpInput, pefInput;

    private FirebaseFirestore db;
    private String selectedChildUid;
    private String parentUid;

    // Red flags
    private boolean cantSpeakFullSentences;
    private boolean chestRetractions;
    private boolean blueLipsNails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optional_data);

        chooseChildButton = findViewById(R.id.chooseChildButton);
        followUpInput = findViewById(R.id.followUpInput);
        pefInput = findViewById(R.id.pefInput);
        Button saveButton = findViewById(R.id.saveButton);
        Button backButton = findViewById(R.id.backButton);
        Button nextButton = findViewById(R.id.nextButton);
        Button recordMedicationButton = findViewById(R.id.recordMedicationButton);

        db = FirebaseFirestore.getInstance();

        // Retrieve extras from intent
        Intent incoming = getIntent();
        parentUid = incoming.getStringExtra("PARENT_UID");
        cantSpeakFullSentences = incoming.getBooleanExtra("cantSpeakFullSentences", false);
        chestRetractions = incoming.getBooleanExtra("chestRetractions", false);
        blueLipsNails = incoming.getBooleanExtra("blueLipsNails", false);

        if (parentUid == null) {
            Toast.makeText(this, "Parent UID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Child selection dialog
        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog dialog = new ChildDiaglog(this, provider);
        chooseChildButton.setOnClickListener(v -> dialog.showSelectionDialog(chooseChildButton));

        saveButton.setOnClickListener(v -> saveTriageLog());

        // Record Medication button -> passes parent UID and red flags
        recordMedicationButton.setOnClickListener(v -> {
            Object tag = chooseChildButton.getTag();
            if (tag == null) {
                Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedChildUid = tag.toString();
            Intent intent = new Intent(OptionalDataActivity.this, RecordMedicationTriage.class);
            intent.putExtra("CHILD_ID", selectedChildUid);
            intent.putExtra("returnClass", OptionalDataActivity.this.getClass().getName());
            intent.putExtra("PARENT_UID", parentUid);

            intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            intent.putExtra("chestRetractions", chestRetractions);
            intent.putExtra("blueLipsNails", blueLipsNails);

            startActivity(intent);
        });

        // Back button: sends parent UID + red flags back to previous activity
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RedFlagsActivity.class);
            intent.putExtra("PARENT_UID", parentUid);
            intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            intent.putExtra("chestRetractions", chestRetractions);
            intent.putExtra("blueLipsNails", blueLipsNails);
            startActivity(intent);
        });

        nextButton.setOnClickListener(v -> {
            Object tag = chooseChildButton.getTag();
            if (tag == null) {
                Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedChildUid = tag.toString();

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            DocumentReference triageRef = db.collection("users")
                    .document(parentUid)
                    .collection("children")
                    .document(selectedChildUid)
                    .collection("triage")
                    .document("logs")
                    .collection("entries")
                    .document(today);

            triageRef.get().addOnSuccessListener(doc -> {

                // --- CASE 1: Today's triage exists and has a valid zone ---
                if (doc.exists()) {
                    String triageZone = doc.getString("zone");
                    if (triageZone != null) {
                        launchZoneActivity(triageZone);
                        return;
                    }
                }

                // --- CASE 2: No triage OR no zone -> fall back to PEF/latest ---
                DocumentReference latestPEFRef = db.collection("users")
                        .document(parentUid)
                        .collection("children")
                        .document(selectedChildUid)
                        .collection("PEF")
                        .document("latest");

                latestPEFRef.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String zone = snapshot.getString("zone");
                        if (zone != null) {
                            launchZoneActivity(zone);
                        } else {
                            Toast.makeText(this, "Zone unavailable", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No triage or PEF data found", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch PEF data: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );

            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Failed to fetch triage data: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });

    }


    private void saveTriageLog() {
        Object tag = chooseChildButton.getTag();
        if (tag != null) selectedChildUid = tag.toString();

        if (selectedChildUid == null) {
            Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = followUpInput.getText().toString().trim();
        String dateId = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DocumentReference logRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(selectedChildUid)
                .collection("triage")
                .document("logs")
                .collection("entries")
                .document(dateId);

        Map<String, Object> data = new HashMap<>();
        if (!message.isEmpty()) data.put("message-triage", message);
        data.put("timestamp", System.currentTimeMillis());

        // Include red flags
        data.put("cantSpeakFullSentences", cantSpeakFullSentences);
        data.put("chestRetractions", chestRetractions);
        data.put("blueLipsNails", blueLipsNails);

        String pefStr = pefInput.getText().toString().trim();
        if (!pefStr.isEmpty()) {
            try {
                double dailyPEF = Double.parseDouble(pefStr);
                data.put("dailyPEF", dailyPEF);

                DocumentReference pbRef = db.collection("users")
                        .document(parentUid)
                        .collection("children")
                        .document(selectedChildUid);

                pbRef.get().addOnSuccessListener(snapshot -> {
                    Double pb = snapshot.getDouble("pb");
                    if (pb != null && pb > 0) {
                        String zone = computeZone(dailyPEF, pb);
                        data.put("zone", zone);
                        logRef.set(data)
                                .addOnSuccessListener(a -> Toast.makeText(this, "Triage log saved", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save triage log: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        fetchLatestZoneAndSave(logRef, data);
                    }
                }).addOnFailureListener(e -> fetchLatestZoneAndSave(logRef, data));

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid PEF value", Toast.LENGTH_SHORT).show();
            }
        } else {
            fetchLatestZoneAndSave(logRef, data);
        }
    }

    private void fetchLatestZoneAndSave(DocumentReference logRef, Map<String, Object> data) {
        DocumentReference latestRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(selectedChildUid)
                .collection("PEF")
                .document("latest");

        latestRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String zone = snapshot.getString("zone");
                if (zone != null) data.put("zone", zone);
            }
            logRef.set(data)
                    .addOnSuccessListener(a -> Toast.makeText(OptionalDataActivity.this, "Triage log saved", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(OptionalDataActivity.this, "Failed to save triage log: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }).addOnFailureListener(e -> logRef.set(data)
                .addOnSuccessListener(a -> Toast.makeText(OptionalDataActivity.this, "Triage log saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(err -> Toast.makeText(OptionalDataActivity.this, "Failed to save triage log: " + err.getMessage(), Toast.LENGTH_LONG).show())
        );
    }

    private String computeZone(double dailyPEF, double pb) {
        double percent = (dailyPEF / pb) * 100;
        if (percent >= 80) return "GREEN";
        if (percent >= 50) return "YELLOW";
        return "RED";
    }

    private void launchZoneActivity(String zone) {
        Intent intent;

        switch (zone.toUpperCase()) {
            case "GREEN":
                intent = new Intent(OptionalDataActivity.this, GreenCardActivity.class);
                break;

            case "YELLOW":
                intent = new Intent(OptionalDataActivity.this, YellowCardActivity.class);
                break;

            case "RED":
                intent = new Intent(OptionalDataActivity.this, RedCardActivity.class);
                break;

            default:
                Toast.makeText(this, "Invalid zone: " + zone, Toast.LENGTH_SHORT).show();
                return;
        }

        // Pass required extras
        intent.putExtra("PARENT_UID", parentUid);
        intent.putExtra("CHILD_ID", selectedChildUid);
        intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
        intent.putExtra("chestRetractions", chestRetractions);
        intent.putExtra("blueLipsNails", blueLipsNails);
        intent.putExtra(GreenCardActivity.EXTRA_IS_CHILD, 0);

        startActivity(intent);
    }

}
