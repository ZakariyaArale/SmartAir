package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RecordMedicationTriage extends AppCompatActivity {

    private String childUid;
    private String parentUid;
    private String returnClassName;

    // Red flags
    private boolean cantSpeakFullSentences;
    private boolean chestRetractions;
    private boolean blueLipsNails;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triage_medication);

        // Retrieve extras
        Intent incoming = getIntent();
        childUid = incoming.getStringExtra("CHILD_ID");
        parentUid = incoming.getStringExtra("PARENT_UID");

        /*
        returnClassName = incoming.getStringExtra("returnClass");

        cantSpeakFullSentences = incoming.getBooleanExtra("cantSpeakFullSentences", false);
        chestRetractions = incoming.getBooleanExtra("chestRetractions", false);
        blueLipsNails = incoming.getBooleanExtra("blueLipsNails", false);
        */

        if (childUid == null || parentUid == null) {
            Toast.makeText(this, "Missing parent or child UID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        db = FirebaseFirestore.getInstance();

        Button chooseMedButton = findViewById(R.id.chooseMedButton);
        EditText doseInput = findViewById(R.id.rescueInput);
        Button saveButton = findViewById(R.id.saveButton);
        Button backButton = findViewById(R.id.backButton);

        // Medication selection dialog
        MedicationDialog dialog = new MedicationDialog(this, new FireBaseProcessMedication(parentUid,childUid));
        chooseMedButton.setOnClickListener(v -> dialog.showSelectionDialog(chooseMedButton));

        // Save button
        saveButton.setOnClickListener(v -> saveMedication(chooseMedButton, doseInput));

        // Back button: sends parent UID and red flags back
        backButton.setOnClickListener(v -> {

            finish();

            /*
            if (returnClassName != null && !returnClassName.isEmpty()) {
                try {
                    Class<?> returnClass = Class.forName(returnClassName);
                    Intent intent = new Intent(RecordMedicationTriage.this, returnClass);
                    intent.putExtra("PARENT_UID", parentUid);
                    intent.putExtra("CHILD_ID", childUid);
                    intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
                    intent.putExtra("chestRetractions", chestRetractions);
                    intent.putExtra("blueLipsNails", blueLipsNails);
                    startActivity(intent);
                    finish();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Return class not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                finish();
            }

             */
        });
    }

    private void saveMedication(Button chooseMedButton, EditText doseInput) {
        Object medTag = chooseMedButton.getTag();
        if (medTag == null) {
            Toast.makeText(this, "Select a medication first", Toast.LENGTH_SHORT).show();
            return;
        }

        String medUUID = medTag.toString();
        String medName = chooseMedButton.getText().toString().trim();

        String doseStr = doseInput.getText().toString().trim();
        if (doseStr.isEmpty()) {
            Toast.makeText(this, "Enter dose amount", Toast.LENGTH_SHORT).show();
            return;
        }

        int doseTaken;
        try {
            doseTaken = Integer.parseInt(doseStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid dose amount", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();

        // Make a med log to keep track of medication usage
        // fetch isRescue
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .collection("medications")
                .document(medUUID)
                .get()
                .addOnSuccessListener(doc -> {

                    boolean isRescue = false;
                    if (doc.exists() && doc.getBoolean("isRescue") != null) {
                        isRescue = doc.getBoolean("isRescue");
                    }

                    // Build medLog entry
                    Map<String, Object> medLog = new HashMap<>();
                    medLog.put("timestamp", now);
                    medLog.put("doseCount", doseTaken);
                    medLog.put("medId", medUUID);
                    medLog.put("childId", childUid);

                    // triage has no pre/post feelings, so default values used
                    medLog.put("preFeeling", -1);
                    medLog.put("postFeeling", -1);
                    medLog.put("feelingChange", "N/A");

                    medLog.put("isRescue", isRescue);

                    // Save inside medLogs collection
                    db.collection("users")
                            .document(parentUid)
                            .collection("children")
                            .document(childUid)
                            .collection("medLogs")
                            .add(medLog)
                            .addOnSuccessListener(ref ->
                                    Toast.makeText(this, "Medication logged in medLogs", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error saving med log: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                });

        //updates puff count
        DocumentReference medRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .collection("medications")
                .document(medUUID);

        medRef.get().addOnSuccessListener(snapshot -> {
            Long currentPuffs = snapshot.getLong("puffsLeft");
            if (currentPuffs == null) currentPuffs = 0L;
            medRef.update("puffsLeft", Math.max(currentPuffs - doseTaken, 0));
        });

    }


    private void resetForm(Button chooseMedButton, EditText doseInput) {
        Toast.makeText(this, "Medication logged in triage", Toast.LENGTH_SHORT).show();
        doseInput.setText("");
        chooseMedButton.setText("Choose Medication");
        chooseMedButton.setTag(null);
    }
}
