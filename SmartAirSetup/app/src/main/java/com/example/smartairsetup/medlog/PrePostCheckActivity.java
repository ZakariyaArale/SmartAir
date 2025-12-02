package com.example.smartairsetup.medlog;

import static com.example.smartairsetup.notification.RapidRescueCountHelper.checkRescueRepeats;

import android.app.Activity;
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

import com.example.smartairsetup.R;
import com.example.smartairsetup.child_home_ui.ChildHomeActivity;
import com.example.smartairsetup.notification.AlertHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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

    private void setNextButton() {
        Button nextButton = findViewById(R.id.checkInNextButton);

        nextButton.setOnClickListener(v -> {

            if (mode != null && mode.equals("post")) {

                // Fetch medication to confirm if it's a rescue med
                final boolean[] isRescue = {false};   // <-- FIX HERE

                db.collection("users")
                        .document(parentUid)
                        .collection("children")
                        .document(childId)
                        .collection("medications")
                        .document(medID)
                        .get()
                        .addOnSuccessListener(doc -> {

                            if (doc.exists() && doc.getBoolean("isRescue") != null) {
                                isRescue[0] = doc.getBoolean("isRescue");
                            }

                            // Build med log entry
                            Map<String, Object> medLog = new HashMap<>();
                            medLog.put("timestamp", passedTimestamp);
                            medLog.put("doseCount", passedDoseCount);
                            medLog.put("medId", medID);
                            medLog.put("childId", childId);
                            medLog.put("preFeeling", passedFeeling);
                            medLog.put("postFeeling", selected);
                            medLog.put("isRescue", isRescue[0]);

                            String feelingChange = feelingSpinner.getSelectedItem().toString();
                            medLog.put("feelingChange", feelingChange);

                            // Immediate alert if Worse after dose
                            if (feelingChange.equals("Worse")) {
                                AlertHelper.sendAlertToParent(parentUid, childId, "WORSE_AFTER_DOSE", this);
                            }

                            // Save the log FIRST
                            db.collection("users")
                                    .document(parentUid)
                                    .collection("children")
                                    .document(childId)
                                    .collection("medLogs")
                                    .add(medLog)
                                    .addOnSuccessListener(docRef -> {

                                        // ONLY check rapid rescue repeats if rescue
                                        if (isRescue[0]) {
                                            checkRescueRepeats(parentUid, childId, moreThanTwo -> {
                                                if (moreThanTwo) {
                                                    AlertHelper.sendAlertToParent(parentUid, childId, "RESCUE_REPEATED", this);
                                                }
                                            });
                                        }

                                        // Update medication puffs left
                                        DocumentReference medRef = db.collection("users")
                                                .document(parentUid)
                                                .collection("children")
                                                .document(childId)
                                                .collection("medications")
                                                .document(medID);

                                        medRef.get().addOnSuccessListener(snapshot -> {
                                            Long puffs = snapshot.getLong("puffsLeft");
                                            if (puffs == null) puffs = 0L;
                                            medRef.update("puffsLeft", Math.max(puffs - passedDoseCount, 0));
                                        });

                                        Toast.makeText(this, "Medication log saved!", Toast.LENGTH_SHORT).show();

                                        Intent i = new Intent(this, ChildHomeActivity.class);
                                        i.putExtra("CHILD_ID", childId);
                                        startActivity(i);
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Failed to save log", Toast.LENGTH_SHORT).show()
                                    );
                        });

            } else {
                // PRE mode
                Intent i = new Intent(this, RecordMedUsageActivity.class);
                i.putExtra("CHILD_ID", childId);
                i.putExtra("PRE_FEELING", selected);
                startActivity(i);
            }
        });
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