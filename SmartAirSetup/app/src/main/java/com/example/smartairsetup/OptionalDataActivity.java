package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class OptionalDataActivity extends AppCompatActivity {

    private ChildStorage emergencyStorage;
    private IntegerDataParse intParser;

    private Button chooseChildButton;
    private EditText rescueInput;
    private EditText pefInput;
    private Button saveButton;

    private double pbrate;
    private String globalTriageZone = ""; // ✅ Global triage zone

    private FirebaseFirestore db;
    private final String parentID = "VfB95gwXXyWFAqdajTHJBgyeYfB3"; // Hardcoded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optional_data);

        // Initialize parsers and storage
        intParser = new IntegerDataParse();
        emergencyStorage = new ChildStorage();

        // Initialize UI
        chooseChildButton = findViewById(R.id.chooseChildButton);
        rescueInput = findViewById(R.id.rescueInput);
        pefInput = findViewById(R.id.pefInput);
        saveButton = findViewById(R.id.saveButton);
        Button backButton = findViewById(R.id.backButton);
        Button nextButton = findViewById(R.id.nextButton);

        db = FirebaseFirestore.getInstance();

        // Child selection dialog
        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog childDiaglog = new ChildDiaglog(this, provider);
        chooseChildButton.setOnClickListener(v -> childDiaglog.showSelectionDialog(chooseChildButton));

        // Save button logic
        saveButton.setOnClickListener(v -> saveEmergencyCheck());
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(OptionalDataActivity.this, RedFlagsActivity.class);
            startActivity(intent);
        });

        nextButton.setOnClickListener(v -> {
            if (globalTriageZone.isEmpty()) {
                Toast.makeText(OptionalDataActivity.this, "Compute triage zone first", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent;
            switch (globalTriageZone) {
                case "Green":
                    intent = new Intent(OptionalDataActivity.this, GreenCardActivity.class);
                    break;
                case "Yellow":
                    intent = new Intent(OptionalDataActivity.this, YellowCardActivity.class);
                    break;
                case "Red":
                    intent = new Intent(OptionalDataActivity.this, RedCardActivity.class);
                    break;
                default:
                    Toast.makeText(OptionalDataActivity.this, "Invalid triage zone", Toast.LENGTH_SHORT).show();
                    return;
            }

            startActivity(intent);
        });
    }

    private void saveEmergencyCheck() {
        String childUid = getSelectedChildUid();
        if (childUid == null) return;

        int rescueAttempts = intParser.parsePEF(rescueInput);
        int pefTriageInput = intParser.parsePEF(pefInput);

        if (rescueAttempts <= 0) {
            Toast.makeText(this, "Please enter number of rescue attempts", Toast.LENGTH_SHORT).show();
            return;
        }

        saveEmergencyData(childUid, rescueAttempts, pefTriageInput);
    }

    private String getSelectedChildUid() {
        Object tag = chooseChildButton.getTag();
        if (tag == null) {
            Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
            return null;
        }
        return tag.toString();
    }

    private void saveEmergencyData(String childUid, int rescueAttempts, int pefInputValue) {
        // Load PB and daily PEF from Firestore
        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;

                    Double pb = documentSnapshot.getDouble("pb");
                    Double dailyPef = documentSnapshot.getDouble("pef");
                    Double pefTriage = (double) pefInputValue;

                    // 1️⃣ PB check
                    if (pb == null || pb == 0) {
                        Toast.makeText(this, "Go to PB section and save PB", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 2️⃣ PEF fallback
                    if (pefTriage == 0) {
                        if (dailyPef == null || dailyPef == 0) {
                            Toast.makeText(this, "Daily PEF isn't given; provide PEF-Triage or Daily PEF", Toast.LENGTH_LONG).show();
                            return;
                        } else {
                            pefTriage = dailyPef;
                        }
                    }

                    // 3️⃣ Compute PB rate
                    pbrate = pefTriage / pb;

                    // 4️⃣ Determine triage zone
                    if (pbrate >= 0.8) {
                        globalTriageZone = "Green";
                    } else if (pbrate >= 0.5) {
                        globalTriageZone = "Yellow";
                    } else {
                        globalTriageZone = "Red";
                    }

                    // 5️⃣ Save rescue, pef, and triage zone to Firestore
                    Map<String, Object> data = new HashMap<>();
                    data.put("rescue-triage", rescueAttempts);
                    data.put("pef-triage", pefTriage);
                    data.put("triage-zone", globalTriageZone);

                    db.collection("users")
                            .document(parentID)
                            .collection("children")
                            .document(childUid)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Data saved. Zone: " + globalTriageZone, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading PB/daily PEF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}