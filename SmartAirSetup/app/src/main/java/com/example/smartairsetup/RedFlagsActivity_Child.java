package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class RedFlagsActivity_Child extends AppCompatActivity {

    private RadioGroup radioSpeakFullSentences, radioChestRetractions, radioBlueLipsNails;
    private Button backButton, nextButton;

    private FirebaseFirestore db;

    // Hardcoded test UIDs
    private final String childUid = "gifrbhr98mAAyv78MC80";
    private final String parentUid = "VfB95gwXXyWFAqdajTHJBgyeYfB3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_red_flags_child);

        db = FirebaseFirestore.getInstance();

        // Initialize radio groups
        radioSpeakFullSentences = findViewById(R.id.radioSpeakFullSentences);
        radioChestRetractions = findViewById(R.id.radioChestRetractions);
        radioBlueLipsNails = findViewById(R.id.radioBlueLipsNails);

        // Initialize buttons
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);

        backButton.setOnClickListener(v -> finish());
        nextButton.setOnClickListener(v -> handleNextButton());
    }

    private void handleNextButton() {
        // Ensure all questions are answered
        if (radioSpeakFullSentences.getCheckedRadioButtonId() == -1 ||
                radioChestRetractions.getCheckedRadioButtonId() == -1 ||
                radioBlueLipsNails.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check red flags
        boolean cantSpeakFullSentences = ((RadioButton)findViewById(R.id.radioSpeakNo)).isChecked();
        boolean chestRetractions = ((RadioButton)findViewById(R.id.radioChestYes)).isChecked();
        boolean blueLipsNails = ((RadioButton)findViewById(R.id.radioBlueYes)).isChecked();
        boolean anyRedFlags = cantSpeakFullSentences || chestRetractions || blueLipsNails;

        if (anyRedFlags) {
            // Child emergency
            startActivity(new Intent(RedFlagsActivity_Child.this, EmergencyActivity_Child.class));
        } else {
            // Fetch latest zone from parent's record
            db.collection("users")
                    .document(parentUid)
                    .collection("children")
                    .document(childUid)
                    .collection("PEF")
                    .document("latest")
                    .get()
                    .addOnSuccessListener(latestDoc -> {
                        if (!latestDoc.exists()) {
                            Toast.makeText(this, "Please ask parent to enter your PEF and Parent", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String zone = latestDoc.getString("zone");
                        if (zone == null) {
                            Toast.makeText(this, "Please ask parent to enter your PEF and Parent", Toast.LENGTH_LONG).show();
                            return;
                        }

                        Intent intent;
                        switch (zone.toUpperCase()) {
                            case "GREEN":
                                intent = new Intent(RedFlagsActivity_Child.this, GreenCardActivity.class);
                                break;
                            case "YELLOW":
                                intent = new Intent(RedFlagsActivity_Child.this, YellowCardActivity.class);
                                break;
                            case "RED":
                                intent = new Intent(RedFlagsActivity_Child.this, RedCardActivity.class);
                                break;
                            default:
                                Toast.makeText(this, "Please ask parent to enter your PEF and Parent", Toast.LENGTH_LONG).show();
                                return;
                        }
                        startActivity(intent);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error fetching zone: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
