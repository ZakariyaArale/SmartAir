package com.example.smartairsetup.triage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.child_home_ui.ChildHomeActivity;
import com.example.smartairsetup.R;
import com.example.smartairsetup.notification.AlertHelper;

public class RedFlagsActivity_Child extends AppCompatActivity {

    private static final String TAG = "RedFlagsActivity_Child";

    private RadioGroup radioSpeakFullSentences;
    private RadioGroup radioChestRetractions;
    private RadioGroup radioBlueLipsNails;

    private Button backButton;
    private Button nextButton;

    private String parentUid;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_red_flags_child);

        //Retrieve intent data
        parentUid = getIntent().getStringExtra("PARENT_UID");
        childId = getIntent().getStringExtra("CHILD_ID");

        Log.d(TAG, "onCreate: parentUid=" + parentUid + ", childId=" + childId);

        if (parentUid == null || parentUid.isEmpty()) {
            Toast.makeText(this, "Missing parent UID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Missing child UID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Initialize UI elements
        radioSpeakFullSentences = findViewById(R.id.radioSpeakFullSentences);
        radioChestRetractions = findViewById(R.id.radioChestRetractions);
        radioBlueLipsNails = findViewById(R.id.radioBlueLipsNails);

        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);

        //Back button
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChildHomeActivity.class);
            intent.putExtra("PARENT_UID", parentUid);
            intent.putExtra("CHILD_ID", childId);
            startActivity(intent);
            finish();
        });

        //Next button
        nextButton.setOnClickListener(v -> handleNextClicked());
    }

    private void handleNextClicked() {

        // Ensure all questions answered
        if (radioSpeakFullSentences.getCheckedRadioButtonId() == -1 ||
                radioChestRetractions.getCheckedRadioButtonId() == -1 ||
                radioBlueLipsNails.getCheckedRadioButtonId() == -1) {

            Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract boolean answers
        boolean cantSpeakFullSentences = ((RadioButton) findViewById(R.id.radioSpeakNo)).isChecked();
        boolean chestRetractions = ((RadioButton) findViewById(R.id.radioChestYes)).isChecked();
        boolean blueLipsNails = ((RadioButton) findViewById(R.id.radioBlueYes)).isChecked();

        // Decide next screen
        Intent intent;
        if (cantSpeakFullSentences || chestRetractions || blueLipsNails) {
            intent = new Intent(this, EmergencyActivity_Child.class);
            AlertHelper.sendAlertToParent(parentUid, childId, "TRIAGE_ESCALATION", this);
        } else {
            intent = new Intent(this, OptionalDataActivity_Child.class);
        }

        // Pass required data forward
        intent.putExtra("PARENT_UID", parentUid);
        intent.putExtra("CHILD_ID", childId); // <-- consistent key
        intent.putExtra("cantSpeakFullSentences", cantSpeakFullSentences);
        intent.putExtra("chestRetractions", chestRetractions);
        intent.putExtra("blueLipsNails", blueLipsNails);

        Log.d(TAG, "handleNextClicked: Sending childId=" + childId);

        startActivity(intent);
    }
}
