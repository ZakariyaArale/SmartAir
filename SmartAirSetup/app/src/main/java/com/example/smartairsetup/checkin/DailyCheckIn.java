package com.example.smartairsetup.checkin;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartairsetup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DailyCheckIn extends AppCompatActivity {

    private RadioGroup radioNightWaking;
    private RadioGroup radioActivityLimits;
    private RadioGroup radioCoughWheeze;
    private TextView textCheckInError;
    private Button buttonSubmit;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Triggers
    private CheckBox checkTriggerExercise;
    private CheckBox checkTriggerColdAir;
    private CheckBox checkTriggerDustPets;
    private CheckBox checkTriggerSmoke;
    private CheckBox checkTriggerIllness;
    private CheckBox checkTriggerOdors;
    private String currentAuthorLabel = null; // "Parent-entered" or "Child-entered"
    private String currentRole = null;        // "parent" / "child" / "provider"
    public static final String EXTRA_CHILD_ID = "extra_child_id";
    public static final String EXTRA_CHILD_NAME = "extra_child_name";
    private String childId;
    private String childName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_check_in);
        Intent intent = getIntent();
        childId = intent.getStringExtra(EXTRA_CHILD_ID);
        childName = intent.getStringExtra(EXTRA_CHILD_NAME);

        if (childId == null) {
            Toast.makeText(this, "No child selected for check-in.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        TextView childNameText = findViewById(R.id.textChildNameForCheckin);
        if (childNameText != null && childName != null) {
            childNameText.setText("Check-in for " + childName);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserRole();

        radioNightWaking = findViewById(R.id.radioNightWaking);
        radioActivityLimits = findViewById(R.id.radioActivityLimits);
        radioCoughWheeze = findViewById(R.id.radioCoughWheeze);
        textCheckInError = findViewById(R.id.textCheckInError);
        buttonSubmit = findViewById(R.id.buttonSubmitCheckIn);
        Button buttonBack = findViewById(R.id.buttonBack);
        checkTriggerExercise = findViewById(R.id.checkTriggerExercise);
        checkTriggerColdAir = findViewById(R.id.checkTriggerColdAir);
        checkTriggerDustPets = findViewById(R.id.checkTriggerDustPets);
        checkTriggerSmoke = findViewById(R.id.checkTriggerSmoke);
        checkTriggerIllness = findViewById(R.id.checkTriggerIllness);
        checkTriggerOdors = findViewById(R.id.checkTriggerOdors);

        buttonSubmit.setOnClickListener(v -> submitCheckIn());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void submitCheckIn() {
        textCheckInError.setVisibility(View.GONE);
        textCheckInError.setText("");

        if (mAuth.getCurrentUser() == null) {
            textCheckInError.setText("You must be logged in to complete a check-in.");
            textCheckInError.setVisibility(View.VISIBLE);
            return;
        }

        String nightWaking = getSelectedValue(radioNightWaking);
        String activityLimits = getSelectedValue(radioActivityLimits);
        String coughWheeze = getSelectedValue(radioCoughWheeze);

        if (nightWaking == null || activityLimits == null || coughWheeze == null) {
            textCheckInError.setText("Please answer all questions.");
            textCheckInError.setVisibility(View.VISIBLE);
            return;
        }

        buttonSubmit.setEnabled(false);

        String uid = mAuth.getCurrentUser().getUid();

        String dateId = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        Map<String, Object> data = new HashMap<>();
        data.put("date", dateId);
        data.put("nightWaking", nightWaking);
        data.put("activityLimits", activityLimits);
        data.put("coughWheeze", coughWheeze);
        data.put("createdAt", new Date());
        data.put("childId", childId);
        data.put("childName", childName);

        String authorLabel = currentAuthorLabel;
        if (authorLabel == null) {
            // Fallback if Firestore doc hasn't loaded yet
            authorLabel = "Unknown";
        }

        data.put("authorLabel", authorLabel);
        data.put("authorRole", currentRole); // optional, might be useful later

        List<String> triggers = new ArrayList<>();
        if (checkTriggerExercise.isChecked()) triggers.add("exercise");
        if (checkTriggerColdAir.isChecked()) triggers.add("cold_air");
        if (checkTriggerDustPets.isChecked()) triggers.add("dust_pets");
        if (checkTriggerSmoke.isChecked()) triggers.add("smoke");
        if (checkTriggerIllness.isChecked()) triggers.add("illness");
        if (checkTriggerOdors.isChecked()) triggers.add("strong_odors");

        data.put("triggers", triggers);

        db.collection("users")
                .document(uid)
                .collection("dailyCheckins")
                .add(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Check-in saved",
                            Toast.LENGTH_SHORT).show();
                    buttonSubmit.setEnabled(true);
                    finish(); // go back to previous screen
                })
                .addOnFailureListener(e -> {
                    textCheckInError.setText("Failed to save check-in: " + e.getMessage());
                    textCheckInError.setVisibility(View.VISIBLE);
                    buttonSubmit.setEnabled(true);
                });

    }

    private void loadUserRole() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        currentRole = snapshot.getString("role");
                        if ("parent".equals(currentRole)) {
                            currentAuthorLabel = "Parent-entered";
                        } else if ("child".equals(currentRole)) {
                            currentAuthorLabel = "Child-entered";
                        } else {
                            currentAuthorLabel = "Unknown";
                        }
                    }
                });
    }


    private @Nullable String getSelectedValue(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return null;
        RadioButton rb = findViewById(id);
        if (rb == null) return null;

        String text = rb.getText().toString().toLowerCase(Locale.getDefault());
        // Map display text to compact values if you want:
        if (text.contains("none")) return "none";
        if (text.contains("some")) return "some";
        if (text.contains("lot"))  return "a_lot";
        return text;
    }

}