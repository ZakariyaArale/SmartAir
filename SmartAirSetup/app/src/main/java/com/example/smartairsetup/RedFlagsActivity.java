package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RedFlagsActivity extends AppCompatActivity {

    private RadioGroup radioSpeakFullSentences;
    private RadioGroup radioChestRetractions;
    private RadioGroup radioBlueLipsNails;

    private Button backButton;
    private Button nextButton;

    private String parentUid; // receive from intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_red_flags);

        // Receive parent UID from intent
        parentUid = getIntent().getStringExtra("PARENT_UID");
        if (parentUid == null) {
            Toast.makeText(this, "Missing parent ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize RadioGroups
        radioSpeakFullSentences = findViewById(R.id.radioSpeakFullSentences);
        radioChestRetractions = findViewById(R.id.radioChestRetractions);
        radioBlueLipsNails = findViewById(R.id.radioBlueLipsNails);

        // Initialize Buttons
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);

        // Back button simply finishes this activity
        backButton.setOnClickListener(v -> finish());

        // Next button
        nextButton.setOnClickListener(v -> {
            // Ensure all questions answered
            if (radioSpeakFullSentences.getCheckedRadioButtonId() == -1 ||
                    radioChestRetractions.getCheckedRadioButtonId() == -1 ||
                    radioBlueLipsNails.getCheckedRadioButtonId() == -1) {

                Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check each radio button directly
            boolean cantSpeakFullSentences = ((RadioButton) findViewById(R.id.radioSpeakNo)).isChecked();
            boolean chestRetractions = ((RadioButton) findViewById(R.id.radioChestYes)).isChecked();
            boolean blueLipsNails = ((RadioButton) findViewById(R.id.radioBlueYes)).isChecked();

            // Launch correct activity and pass parent UID
            Intent intent;
            if (cantSpeakFullSentences || chestRetractions || blueLipsNails) {
                intent = new Intent(this, EmergencyActivity.class);
            } else {
                intent = new Intent(this, OptionalDataActivity.class);
            }
            // Pass the parent UID along
            intent.putExtra("PARENT_UID", parentUid);
            intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
            intent.putExtra("chestRetractions", chestRetractions);
            intent.putExtra("blueLipsNails", blueLipsNails);
            startActivity(intent);
        });
    }
}
