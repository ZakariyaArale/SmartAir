package com.example.smartairsetup.medlog;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrePostCheckActivity extends AppCompatActivity {

    // UI
    private TextView checkInTitleTV, checkFeelingTitleTV;
    private Button nextButton;
    private RadioGroup segmentGroup;
    private Spinner feelingSpinner;

    // Radio buttons
    private RadioButton opt1, opt2, opt3, opt4, opt5;
    private int selectedBreathingScore = 0;

    // Passed values
    private String mode;
    private String childId;
    private int preFeeling;
    private int doseCount;
    private String medID;

    private FirebaseFirestore db;
    private String parentUid;

    private long timestampNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pre_post_check);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        parentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        getIds();
        loadIntentValues();

        if ("post".equals(mode)) setupPostUI();

        setupButtons();
        setupSegmentListener();
    }

    private void loadIntentValues() {
        Intent i = getIntent();
        mode = i.getStringExtra("mode");
        childId = i.getStringExtra("CHILD_ID");

        if (childId == null) {
            Toast.makeText(this, "Missing child info", Toast.LENGTH_LONG).show();
            finish();
        }

        if ("post".equals(mode)) {
            preFeeling = i.getIntExtra("PRE_FEELING", -1);
            doseCount = i.getIntExtra("DOSE_COUNT", 0);
            medID = i.getStringExtra("MED_ID");
        }
    }

    private void getIds() {
        opt1 = findViewById(R.id.opt1);
        opt2 = findViewById(R.id.opt2);
        opt3 = findViewById(R.id.opt3);
        opt4 = findViewById(R.id.opt4);
        opt5 = findViewById(R.id.opt5);
        segmentGroup = findViewById(R.id.BreathingSG);

        checkInTitleTV = findViewById(R.id.medCheckInTitleTV);
        checkFeelingTitleTV = findViewById(R.id.checkFeelingTV);
        nextButton = findViewById(R.id.checkInNextButton);
        feelingSpinner = findViewById(R.id.feelingSpinner);
    }

    private void setupPostUI() {
        checkInTitleTV.setText("Post Medication Check");
        nextButton.setText("Finish");

        checkFeelingTitleTV.setVisibility(TextView.VISIBLE);
        feelingSpinner.setVisibility(TextView.VISIBLE);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                        new String[]{"Worse", "Same", "Better"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        feelingSpinner.setAdapter(adapter);
    }

    private void setupButtons() {
        findViewById(R.id.checkInBackButton).setOnClickListener(v -> finish());

        nextButton.setOnClickListener(v -> {
            if ("pre".equals(mode)) {
                openRecordMedUsage();
            } else {
                savePostLog();
            }
        });
    }

    private void setupSegmentListener() {
        segmentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (opt1.isChecked()) selectedBreathingScore = 1;
            else if (opt2.isChecked()) selectedBreathingScore = 2;
            else if (opt3.isChecked()) selectedBreathingScore = 3;
            else if (opt4.isChecked()) selectedBreathingScore = 4;
            else if (opt5.isChecked()) selectedBreathingScore = 5;

            nextButton.setEnabled(true);
            nextButton.setAlpha(1f);
        });
    }

    private void openRecordMedUsage() {
        Intent i = new Intent(this, RecordMedUsageActivity.class);
        i.putExtra("mode", "post");
        i.putExtra("CHILD_ID", childId);
        i.putExtra("PRE_FEELING", selectedBreathingScore);
        startActivity(i);
    }


    private void savePostLog() {

        timestampNow = System.currentTimeMillis();

        // Fetch medication to see if rescue
        db.collection("users").document(parentUid)
                .collection("children").document(childId)
                .collection("medications").document(medID)
                .get()
                .addOnSuccessListener(doc -> {

                    boolean isRescue = doc.exists() && Boolean.TRUE.equals(doc.getBoolean("isRescue"));

                    Map<String, Object> log = new HashMap<>();
                    log.put("timestamp", timestampNow);
                    log.put("doseCount", doseCount);
                    log.put("medId", medID);
                    log.put("childId", childId);
                    log.put("preFeeling", preFeeling);
                    log.put("postFeeling", selectedBreathingScore);
                    log.put("isRescue", isRescue);

                    String feelingChange = feelingSpinner.getSelectedItem().toString();
                    log.put("feelingChange", feelingChange);

                    if (feelingChange.equals("Worse"))
                        AlertHelper.sendAlertToParent(parentUid, childId, "WORSE_AFTER_DOSE", this);

                    saveLogToFirestore(log, isRescue);
                });
    }

    private void saveLogToFirestore(Map<String, Object> log, boolean isRescue) {

        db.collection("users").document(parentUid)
                .collection("children").document(childId)
                .collection("medLogs")
                .add(log)
                .addOnSuccessListener(ref -> {

                    if (isRescue) checkRapidRescue();

                    updatePuffsLeft();
                    Toast.makeText(this, "Medication log saved!", Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(this, ChildHomeActivity.class);
                    i.putExtra("CHILD_ID", childId);
                    startActivity(i);
                    finish();
                });
    }

    private void checkRapidRescue() {

        Log.d("RESCUE_DEBUG", "------------------------------------");
        Log.d("RESCUE_DEBUG", "checkRapidRescue() CALLED");
        Log.d("RESCUE_DEBUG", "childId = " + childId);
        Log.d("RESCUE_DEBUG", "parentUid = " + parentUid);
        Log.d("RESCUE_DEBUG", "timestampNow = " + timestampNow);

        long buffer = 10 * 1000; // 10-second buffer
        long cutoff = timestampNow - (3 * 60 * 60 * 1000) - buffer;



        db.collection("users").document(parentUid)
                .collection("children").document(childId)
                .collection("medLogs")
                .whereEqualTo("isRescue", true)  // single-field index is automatic
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> recentRescues = new ArrayList<>();


                    for (DocumentSnapshot doc : snapshot) {
                        Long ts = doc.getLong("timestamp");
                        if (ts != null && ts >= cutoff) {
                            recentRescues.add(doc);
                        }
                    }

                    if (recentRescues.size() >= 3) {
                        AlertHelper.sendAlertToParent(parentUid, childId, "RESCUE_REPEATED", this);
                    }
                });
    }


    //40 is used as 40 is 20% of the typical inhaler
    private void updatePuffsLeft() {
        DocumentReference medRef = db.collection("users").document(parentUid)
                .collection("children").document(childId)
                .collection("medications").document(medID);

        medRef.get().addOnSuccessListener(doc -> {
            Long left = doc.getLong("puffsLeft");
            left = (left == null ? 0 : left - doseCount);
            medRef.update("puffsLeft", Math.max(left, 0));
            if (left - doseCount <= 40) {
                AlertHelper.sendAlertToParent(parentUid, childId, "INVENTORY_LOW", this);
            }
        });
    }
}
