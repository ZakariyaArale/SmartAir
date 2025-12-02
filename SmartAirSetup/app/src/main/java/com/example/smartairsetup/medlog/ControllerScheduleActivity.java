package com.example.smartairsetup.medlog;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ControllerScheduleActivity extends AppCompatActivity {

    public static final String EXTRA_CHILD_ID = "extra_child_id";
    public static final String EXTRA_CHILD_NAME = "extra_child_name";

    private FirebaseFirestore db;

    private CheckBox checkMon, checkTue, checkWed, checkThu, checkFri, checkSat, checkSun;
    private EditText editDosesPerDay;

    private String parentUid;
    private String childId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller_schedule);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        String childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);

        if (mAuth.getCurrentUser() == null || childId == null) {
            Toast.makeText(this, "Missing parent or child info.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        parentUid = mAuth.getCurrentUser().getUid();

        TextView textScheduleChildName = findViewById(R.id.textScheduleChildName);
        checkMon = findViewById(R.id.checkMon);
        checkTue = findViewById(R.id.checkTue);
        checkWed = findViewById(R.id.checkWed);
        checkThu = findViewById(R.id.checkThu);
        checkFri = findViewById(R.id.checkFri);
        checkSat = findViewById(R.id.checkSat);
        checkSun = findViewById(R.id.checkSun);
        editDosesPerDay = findViewById(R.id.editDosesPerDay);
        Button buttonSaveSchedule = findViewById(R.id.buttonSaveSchedule);
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        textScheduleChildName.setText(
                getString(R.string.schedule_child_label, childName)
        );

        loadExistingSchedule();

        buttonSaveSchedule.setOnClickListener(v -> saveSchedule());
    }

    private void loadExistingSchedule() {
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medicationSchedule")
                .document("controller")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    checkMon.setChecked(Boolean.TRUE.equals(doc.getBoolean("mon")));
                    checkTue.setChecked(Boolean.TRUE.equals(doc.getBoolean("tue")));
                    checkWed.setChecked(Boolean.TRUE.equals(doc.getBoolean("wed")));
                    checkThu.setChecked(Boolean.TRUE.equals(doc.getBoolean("thu")));
                    checkFri.setChecked(Boolean.TRUE.equals(doc.getBoolean("fri")));
                    checkSat.setChecked(Boolean.TRUE.equals(doc.getBoolean("sat")));
                    checkSun.setChecked(Boolean.TRUE.equals(doc.getBoolean("sun")));

                    Long doses = doc.getLong("dosesPerDay");
                    if (doses != null) {
                        editDosesPerDay.setText(String.valueOf(doses));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load schedule: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void saveSchedule() {
        int doses = 0;
        String dosesText = editDosesPerDay.getText().toString().trim();
        if (!TextUtils.isEmpty(dosesText)) {
            try {
                doses = Integer.parseInt(dosesText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number of doses", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Map<String, Object> schedule = new HashMap<>();
        schedule.put("mon", checkMon.isChecked());
        schedule.put("tue", checkTue.isChecked());
        schedule.put("wed", checkWed.isChecked());
        schedule.put("thu", checkThu.isChecked());
        schedule.put("fri", checkFri.isChecked());
        schedule.put("sat", checkSat.isChecked());
        schedule.put("sun", checkSun.isChecked());
        schedule.put("dosesPerDay", doses);
        schedule.put("updatedAt", new Date());

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medicationSchedule")
                .document("controller")
                .set(schedule)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Schedule saved.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}