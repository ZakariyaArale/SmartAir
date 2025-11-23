package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RedFlagsActivity extends AppCompatActivity {

    private static final String TAG = "RedFlagsActivity";

    private RadioGroup radioSpeakFullSentences;
    private RadioGroup radioChestRetractions;
    private RadioGroup radioBlueLipsNails;

    private Button backButton;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_red_flags);

        // Initialize RadioGroups
        radioSpeakFullSentences = findViewById(R.id.radioSpeakFullSentences);
        radioChestRetractions = findViewById(R.id.radioChestRetractions);
        radioBlueLipsNails = findViewById(R.id.radioBlueLipsNails);

        // Initialize Buttons
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);

        // Debug listeners
        radioSpeakFullSentences.setOnCheckedChangeListener((group, checkedId) ->
                Log.d(TAG, "SpeakFullSentences checkedId=" + checkedId));
        radioChestRetractions.setOnCheckedChangeListener((group, checkedId) ->
                Log.d(TAG, "ChestRetractions checkedId=" + checkedId));
        radioBlueLipsNails.setOnCheckedChangeListener((group, checkedId) ->
                Log.d(TAG, "BlueLipsNails checkedId=" + checkedId));

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Next button
        nextButton.setOnClickListener(v -> {
            Log.d(TAG, "Next button clicked");

            // Ensure all questions answered
            if (radioSpeakFullSentences.getCheckedRadioButtonId() == -1 ||
                    radioChestRetractions.getCheckedRadioButtonId() == -1 ||
                    radioBlueLipsNails.getCheckedRadioButtonId() == -1) {

                Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check each radio button directly
            boolean cantSpeakFullSentences = ((RadioButton)findViewById(R.id.radioSpeakNo)).isChecked();
            boolean chestRetractions = ((RadioButton)findViewById(R.id.radioChestYes)).isChecked();
            boolean blueLipsNails = ((RadioButton)findViewById(R.id.radioBlueYes)).isChecked();

            Log.d(TAG, "Red flags -> cantSpeakFullSentences: " + cantSpeakFullSentences +
                    ", chestRetractions: " + chestRetractions +
                    ", blueLipsNails: " + blueLipsNails);

            // Launch correct activity
            Intent intent;
            if (cantSpeakFullSentences || chestRetractions || blueLipsNails) {
                intent = new Intent(this, EmergencyActivity.class);
            } else {
                intent = new Intent(this, OptionalDataActivity.class);
            }
            startActivity(intent);
        });
    }
}