package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EmergencySelectorActivity extends AppCompatActivity {

    private Button chooseChildButton, saveButton, backButton, nextButton;
    private EditText followUpInput;

    private FirebaseFirestore db;
    private String selectedChildUid;
    private String parentUid = "VfB95gwXXyWFAqdajTHJBgyeYfB3"; // hardcoded parent UID
    private Intent intent;

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

        // Child selection dialog
        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog childDiaglog = new ChildDiaglog(this, provider);

        // If a child is already selected via button tag
        Object tag = chooseChildButton.getTag();
        if (tag != null) {
            selectedChildUid = tag.toString();
            chooseChildButton.setText(selectedChildUid);
        }

        chooseChildButton.setOnClickListener(v -> {
            childDiaglog.showSelectionDialog(chooseChildButton);
            // ChildDiaglog must set chooseChildButton.setTag(selectedChild.uid) when selected
        });

        saveButton.setOnClickListener(v -> saveFollowUp());

        backButton.setOnClickListener(v -> {
            intent = new Intent(this, RedFlagsActivity.class);
            startActivity(intent);
        });

        nextButton.setOnClickListener(v ->
                Toast.makeText(this, "Next screen not implemented", Toast.LENGTH_SHORT).show()
        );
    }

    private void saveFollowUp() {
        Object tag = chooseChildButton.getTag();
        if (tag != null) {
            selectedChildUid = tag.toString();
        }

        if (selectedChildUid == null) {
            Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = followUpInput.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Enter follow-up notes", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("triage-zone", "Emergency");
        data.put("message-triage", message);  // <-- now it's a field
        data.put("timestamp", System.currentTimeMillis());

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(selectedChildUid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Follow-up saved", Toast.LENGTH_SHORT).show();
                    followUpInput.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}