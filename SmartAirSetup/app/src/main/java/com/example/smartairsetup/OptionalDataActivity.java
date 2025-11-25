package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

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
    private String globalTriageZone = "";

    private FirebaseFirestore db;
    private final String parentID = "VfB95gwXXyWFAqdajTHJBgyeYfB3"; // Hardcoded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optional_data);

        intParser = new IntegerDataParse();
        emergencyStorage = new ChildStorage();

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

        // ✅ Save button
        saveButton.setOnClickListener(v -> {
            Log.d("BUTTON", "Save button clicked");
            saveEmergencyCheck();
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(OptionalDataActivity.this, RedFlagsActivity.class);
            startActivity(intent);
        });

        nextButton.setOnClickListener(v -> {
            if (globalTriageZone.isEmpty()) {
                Toast.makeText(this, "Compute triage zone first", Toast.LENGTH_SHORT).show();
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

        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(doc -> {

                    Double pb = doc.getDouble("pb");
                    Double daily = doc.getDouble("pef");

                    if (pb == null || pb == 0) {
                        Toast.makeText(this, "Go to PB section and save PB", Toast.LENGTH_LONG).show();
                        return;
                    }

                    double pefTriage = pefInputValue;
                    if (pefTriage == 0) {
                        if (daily == null || daily == 0) {
                            Toast.makeText(this, "Daily PEF not available. Provide PEF-Triage.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        pefTriage = daily;
                    }

                    // compute pb rate
                    pbrate = pefTriage / pb;

                    if (pbrate >= 0.8) globalTriageZone = "Green";
                    else if (pbrate >= 0.5) globalTriageZone = "Yellow";
                    else globalTriageZone = "Red";

                    // message-triage based on zone
                    String message;
                    switch (globalTriageZone) {
                        case "Green":
                            message = "Green Home Steps";
                            break;
                        case "Yellow":
                            message = "Yellow Home Steps";
                            break;
                        case "Red":
                            message = "Red Home Steps";
                            break;
                        default:
                            message = "";
                            break;
                    }

                    // Firestore log data
                    Map<String, Object> log = new HashMap<>();
                    log.put("rescue-triage", rescueAttempts);
                    log.put("pef-triage", pefTriage);
                    log.put("triage-zone", globalTriageZone);
                    log.put("message-triage", message);
                    log.put("timestamp", System.currentTimeMillis());

                    // --- Create document name using date + timestamp if duplicate
                    long now = System.currentTimeMillis();
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    String dateKey = sdf.format(new java.util.Date(now));
                    String baseDocId = dateKey;

                    db.collection("users")
                            .document(parentID)
                            .collection("children")
                            .document(childUid)
                            .collection("triage")
                            .document("logs")
                            .collection("entries")
                            .document(baseDocId)
                            .get()
                            .addOnSuccessListener(existingDoc -> {

                                String finalDocId;

                                if (existingDoc.exists()) {
                                    // Duplicate → append timestamp
                                    finalDocId = baseDocId + "-" + now;
                                } else {
                                    finalDocId = baseDocId;
                                }

                                db.collection("users")
                                        .document(parentID)
                                        .collection("children")
                                        .document(childUid)
                                        .collection("triage")
                                        .document("logs")
                                        .collection("entries")
                                        .document(finalDocId)
                                        .set(log)
                                        .addOnSuccessListener(a -> {
                                            Toast.makeText(this,
                                                    "Triage logged (" + globalTriageZone + ") as " + finalDocId,
                                                    Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this,
                                                    "Error logging triage: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });

                            });
                });
    }
}
