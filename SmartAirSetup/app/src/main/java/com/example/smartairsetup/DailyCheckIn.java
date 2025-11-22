package com.example.smartairsetup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_check_in);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        radioNightWaking = findViewById(R.id.radioNightWaking);
        radioActivityLimits = findViewById(R.id.radioActivityLimits);
        radioCoughWheeze = findViewById(R.id.radioCoughWheeze);
        textCheckInError = findViewById(R.id.textCheckInError);
        buttonSubmit = findViewById(R.id.buttonSubmitCheckIn);

        buttonSubmit.setOnClickListener(v -> submitCheckIn());
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

        db.collection("users")
                .document(uid)
                .collection("dailyCheckins")
                .document(dateId) // one check-in per day per user
                .set(data)
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

    public static interface ChildFetchListener {
        void onChildrenLoaded(List<UserID> children);
        void onError(Exception e);
    }
}