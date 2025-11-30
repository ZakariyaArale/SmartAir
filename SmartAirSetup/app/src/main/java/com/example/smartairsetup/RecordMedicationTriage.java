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
        returnClassName = incoming.getStringExtra("returnClass");

        cantSpeakFullSentences = incoming.getBooleanExtra("cantSpeakFullSentences", false);
        chestRetractions = incoming.getBooleanExtra("chestRetractions", false);
        blueLipsNails = incoming.getBooleanExtra("blueLipsNails", false);


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

        String dateId = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference triageRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .collection("triage")
                .document("logs")
                .collection("entries")
                .document(dateId);

        Map<String, Object> medData = new HashMap<>();
        medData.put("name", medName);
        medData.put("med_UUID", medUUID);
        medData.put("doseTaken", doseTaken);
        medData.put("timestamp", System.currentTimeMillis());


        triageRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("medications." + medUUID + "." + System.currentTimeMillis(), medData);
                triageRef.update(updateMap)
                        .addOnSuccessListener(a -> resetForm(chooseMedButton, doseInput))
                        .addOnFailureListener(e -> Toast.makeText(this, "Error updating triage: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Map<String, Object> medsMap = new HashMap<>();
                medsMap.put("" + medUUID + "." + System.currentTimeMillis(), medData);
                Map<String, Object> triageInit = new HashMap<>();
                triageInit.put("medications", medsMap);
                triageRef.set(triageInit)
                        .addOnSuccessListener(a -> resetForm(chooseMedButton, doseInput))
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to log medication: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });

        // Update puffsLeft in medication collection
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
