package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PrePostCheckActivity extends AppCompatActivity {

    TextView checkInTitleTV;
    TextView  checkFeelingTitleTV;
    Button nextButton;

    RadioGroup segmentGroup;
    RadioButton opt1;
    RadioButton opt2;
    RadioButton opt3;
    RadioButton opt4;
    RadioButton opt5;
    Spinner feelingSpinner;

    int selected;

    //passed information for log/other uses
    String mode;
    String childId;
    int passedFeeling;
    int passedDoseCount;
    long passedTimestamp;
    String medID;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String parentUid;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pre_post_check);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            parentUid = mAuth.getCurrentUser().getUid();
        } else {
            finish(); // Should never happen, but safe
        }


        getIds();
        Intent intent = getIntent();
        mode = intent.getStringExtra("mode");
        childId = getIntent().getStringExtra("CHILD_ID");

        if(mode != null && mode.equals("post")){
            setUpPostCheck();
            getPassedInfo();
        }

        setBackButton();
        setNextButton();
        setSegmentGroup();

    }

    private void getPassedInfo(){

        //prior to taking medication breath rating 1 = very bad, 5 = very good. (range from 1 -5)
        passedFeeling = getIntent().getIntExtra("PRE_FEELING", -1);
        passedDoseCount = getIntent().getIntExtra("DOSE_COUNT", 0);
        passedTimestamp = getIntent().getLongExtra("TIME_STAMP", 0);
        medID = getIntent().getStringExtra("MED_ID");

    }

    private void getIds(){

        opt1 = findViewById(R.id.opt1);
        opt2 = findViewById(R.id.opt2);
        opt3 = findViewById(R.id.opt3);
        opt4 = findViewById(R.id.opt4);
        opt5 = findViewById(R.id.opt5);
        segmentGroup = findViewById(R.id.BreathingSG);
        checkInTitleTV = findViewById(R.id.medCheckInTitleTV);
        checkFeelingTitleTV = findViewById(R.id.checkFeelingTV);
        nextButton = findViewById(R.id.checkInNextButton);

    }

    private void setUpPostCheck(){

        checkInTitleTV.setText("Post Medication Check");
        nextButton.setText("Finish");

        //makes second question visible
        checkFeelingTitleTV.setVisibility(View.VISIBLE);
        setUpSpinner();

    }

    private void setUpSpinner(){

        feelingSpinner = findViewById(R.id.feelingSpinner);

        feelingSpinner.setVisibility(View.VISIBLE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Worse", "Same", "Better"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        feelingSpinner.setAdapter(adapter);

    }

    private void setBackButton() {
        Button backButton = findViewById(R.id.checkInBackButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                finish();
            });
        }
    }

    private void setNextButton(){
        Button nextButton = findViewById(R.id.checkInNextButton);
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {

                if(mode != null && mode.equals("post")) {

                    // 1️⃣ Fetch medication to get isRescue
                    db.collection("users")
                            .document(parentUid)
                            .collection("children")
                            .document(childId)
                            .collection("medications")
                            .document(medID)
                            .get()
                            .addOnSuccessListener(doc -> {

                                boolean isRescue = false;
                                if (doc.exists() && doc.getBoolean("isRescue") != null) {

                                    isRescue = doc.getBoolean("isRescue");

                                }
                                // Build log entry
                                Map<String, Object> logMedUseData = new HashMap<>();
                                logMedUseData.put("timestamp", passedTimestamp);
                                logMedUseData.put("doseCount", passedDoseCount);
                                logMedUseData.put("medId", medID);
                                logMedUseData.put("childId", childId);
                                logMedUseData.put("preFeeling", passedFeeling);
                                logMedUseData.put("postFeeling", selected);
                                logMedUseData.put("feelingChange",
                                        feelingSpinner.getSelectedItem().toString());
                                logMedUseData.put("isRescue", isRescue);

                                //get is med isRescue

                                // Save to Firebase
                                db.collection("users")
                                        .document(parentUid)
                                        .collection("children")
                                        .document(childId)
                                        .collection("medLogs")
                                        .add(logMedUseData)
                                        .addOnSuccessListener(docRef -> {
                                            Toast.makeText(this, "Medication log saved!", Toast.LENGTH_SHORT).show();
                                            finish(); // go back or go somewhere else
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error saving log", Toast.LENGTH_SHORT).show();
                                            e.printStackTrace();
                                        });

                            });

                    Intent intent = new Intent(this, ChildHomeActivity.class);
                    intent.putExtra("CHILD_ID", childId);
                    startActivity(intent);

                }else{
                    Intent intent = new Intent(this, RecordMedUsageActivity.class);
                    intent.putExtra("CHILD_ID", childId);
                    intent.putExtra("PRE_FEELING", selected); //passes user choice
                    startActivity(intent);
                }

            });

        }

    }

    private void setSegmentGroup(){

        segmentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            selected = 0;
            if(opt1.isChecked()){selected = 1;}
            else if(opt2.isChecked()){selected = 2;}
            else if(opt3.isChecked()){selected = 3;}
            else if(opt4.isChecked()){selected = 4;}
            else if(opt5.isChecked()){selected = 5;}
            nextButton.setEnabled(true);
            nextButton.setAlpha(1f);

        });

    }

}