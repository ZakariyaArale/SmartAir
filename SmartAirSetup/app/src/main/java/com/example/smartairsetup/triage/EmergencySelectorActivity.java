package com.example.smartairsetup.triage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.parent_home_ui.ParentHomeActivity;
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

public class EmergencySelectorActivity extends AppCompatActivity {

    private Button chooseChildButton;
    private Button recordMedicationButton;
    private EditText followUpInput;

    private FirebaseFirestore db;
    private String selectedChildUid;
    private String parentUid;

    // Red flags passed from EmergencyActivity
    private boolean cantSpeakFullSentences;
    private boolean chestRetractions;
    private boolean blueLipsNails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_select);

        chooseChildButton = findViewById(R.id.chooseChildButton);
        followUpInput = findViewById(R.id.followUpInput);
        Button saveButton = findViewById(R.id.saveButton);
        Button backButton = findViewById(R.id.backButton);
        Button nextButton = findViewById(R.id.nextButton);
        recordMedicationButton = findViewById(R.id.recordMedicationButton);

        db = FirebaseFirestore.getInstance();

        // Retrieve parent UID and red flags from EmergencyActivity
        Intent incomingIntent = getIntent();
        parentUid = incomingIntent.getStringExtra("PARENT_UID");
        cantSpeakFullSentences = incomingIntent.getBooleanExtra("cantSpeakFullSentences", false);
        chestRetractions = incomingIntent.getBooleanExtra("chestRetractions", false);
        blueLipsNails = incomingIntent.getBooleanExtra("blueLipsNails", false);

        if (parentUid == null) {
            Toast.makeText(this, "Missing parent ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Child selection dialog
        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog dialog = new ChildDiaglog(this, provider);

        chooseChildButton.setOnClickListener(v -> dialog.showSelectionDialog(chooseChildButton));
        saveButton.setOnClickListener(v -> {
            saveTriageLog();
            recordMedicationButton.setEnabled(true);
            recordMedicationButton.setAlpha(1f);

        });

        // Record Medication button → passes returnClass dynamically
        recordMedicationButton.setOnClickListener(v -> {
            Object tag = chooseChildButton.getTag();
            if (tag == null) {
                Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedChildUid = tag.toString();
            Intent intent = new Intent(EmergencySelectorActivity.this, RecordMedicationTriage.class);
            intent.putExtra("CHILD_ID", selectedChildUid);
            intent.putExtra("PARENT_UID", parentUid);

            Log.d("EmergencySelector", "Launching RecordMedicationTriage: childUid=" + selectedChildUid
                    + ", parentUid=" + parentUid
                    + ", cantSpeakFullSentences=" + cantSpeakFullSentences
                    + ", chestRetractions=" + chestRetractions
                    + ", blueLipsNails=" + blueLipsNails);
            startActivity(intent);
        });

        // Back button → return to EmergencyActivity with parent UID + red flags
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmergencyActivity.class);
            intent.putExtra("PARENT_UID", parentUid);
            intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            intent.putExtra("chestRetractions", chestRetractions);
            intent.putExtra("blueLipsNails", blueLipsNails);
            startActivity(intent);
        });

        // Next button → placeholder, can extend for next workflow
        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ParentHomeActivity.class);
            startActivity(intent);
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
        if (message.isEmpty()) {
            Toast.makeText(this, "Enter follow-up notes", Toast.LENGTH_SHORT).show();
            return;
        }

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
        data.put("zone", "Emergency");
        data.put("message-triage", message);
        data.put("timestamp", System.currentTimeMillis());

        // Record red flags
        data.put("cantSpeakFullSentences", cantSpeakFullSentences);
        data.put("chestRetractions", chestRetractions);
        data.put("blueLipsNails", blueLipsNails);

        logRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Emergency triage logged", Toast.LENGTH_SHORT).show();
                    followUpInput.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
