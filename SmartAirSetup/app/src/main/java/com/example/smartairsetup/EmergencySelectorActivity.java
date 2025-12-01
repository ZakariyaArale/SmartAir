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

public class EmergencySelectorActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyTriage";

    private Button chooseChildButton, saveButton, backButton, nextButton;
    private EditText followUpInput;

    private FirebaseFirestore db;
    private String selectedChildUid;
    private final String parentUid = "VfB95gwXXyWFAqdajTHJBgyeYfB3"; // hardcoded for testing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_select);

        chooseChildButton = findViewById(R.id.chooseChildButton);
        followUpInput = findViewById(R.id.followUpInput);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);

        db = FirebaseFirestore.getInstance();

        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog dialog = new ChildDiaglog(this, provider);

        chooseChildButton.setOnClickListener(v -> dialog.showSelectionDialog(chooseChildButton));

        saveButton.setOnClickListener(v -> {
            saveTriageLog();
        });
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmergencyActivity.class);
            startActivity(intent);
        });

        nextButton.setOnClickListener(v ->
                Toast.makeText(this, "Next screen not implemented", Toast.LENGTH_SHORT).show()
        );
    }

    private void saveTriageLog() {
        // Read selected child UID from button tag
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


        String dateId = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        DocumentReference logRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(selectedChildUid)
                .collection("triage")
                .document("logs")
                .collection("entries")
                .document(dateId);

        Map<String, Object> data = new HashMap<>();
        data.put("triage-zone", "Emergency");
        data.put("message-triage", message);
        data.put("timestamp", System.currentTimeMillis());


        logRef.get().addOnSuccessListener(doc -> {
            logRef.set(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Emergency triage logged", Toast.LENGTH_SHORT).show();
                        followUpInput.setText("");
                        Log.d(TAG, "Logged triage under " + dateId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error writing triage", e);
                    });
        });
    }
}
