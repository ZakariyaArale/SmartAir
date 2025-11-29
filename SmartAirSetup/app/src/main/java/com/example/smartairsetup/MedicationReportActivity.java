package com.example.smartairsetup;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class MedicationReportActivity extends AppCompatActivity {

    public static final String EXTRA_CHILD_ID = "extra_child_id";
    public static final String EXTRA_CHILD_NAME = "extra_child_name";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView textReportChildName;
    private TextView textReportRange;
    private TextView textReportAdherence;

    private String parentUid;
    private String childId;
    private String childName;
    private Button backButton;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_report);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);

        if (mAuth.getCurrentUser() == null || childId == null) {
            Toast.makeText(this, "Missing parent or child information", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        parentUid = mAuth.getCurrentUser().getUid();

        textReportChildName = findViewById(R.id.textReportChildName);
        textReportRange = findViewById(R.id.textReportRange);
        textReportAdherence = findViewById(R.id.textReportAdherence);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        textReportChildName.setText("Child: " + childName);
        textReportRange.setText("Range: last 30 days");

        // Story 2: this is the key call
        loadControllerAdherence();
    }

    // -----------------------------
    // 1) Load schedule
    // 2) Load logs
    // 3) Compute adherence
    // -----------------------------
    private void loadControllerAdherence() {
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medicationSchedule")
                .document("controller")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        textReportAdherence.setText("No controller schedule defined.");
                        return;
                    }

                    boolean mon = Boolean.TRUE.equals(doc.getBoolean("mon"));
                    boolean tue = Boolean.TRUE.equals(doc.getBoolean("tue"));
                    boolean wed = Boolean.TRUE.equals(doc.getBoolean("wed"));
                    boolean thu = Boolean.TRUE.equals(doc.getBoolean("thu"));
                    boolean fri = Boolean.TRUE.equals(doc.getBoolean("fri"));
                    boolean sat = Boolean.TRUE.equals(doc.getBoolean("sat"));
                    boolean sun = Boolean.TRUE.equals(doc.getBoolean("sun"));

                    Set<Integer> plannedWeekdays = new HashSet<>();
                    if (mon) plannedWeekdays.add(Calendar.MONDAY);
                    if (tue) plannedWeekdays.add(Calendar.TUESDAY);
                    if (wed) plannedWeekdays.add(Calendar.WEDNESDAY);
                    if (thu) plannedWeekdays.add(Calendar.THURSDAY);
                    if (fri) plannedWeekdays.add(Calendar.FRIDAY);
                    if (sat) plannedWeekdays.add(Calendar.SATURDAY);
                    if (sun) plannedWeekdays.add(Calendar.SUNDAY);

                    if (plannedWeekdays.isEmpty()) {
                        textReportAdherence.setText("Controller schedule has no planned days.");
                        return;
                    }

                    loadControllerLogsAndCompute(plannedWeekdays);
                })
                .addOnFailureListener(e ->
                        textReportAdherence.setText("Could not load controller schedule: " + e.getMessage())
                );
    }

    private void loadControllerLogsAndCompute(Set<Integer> plannedWeekdays) {
        // Define last 30 days
        Calendar cal = Calendar.getInstance();
        Date end = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        Date start = cal.getTime();

        long startMs = start.getTime();

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medicationLogs_controller")
                .whereGreaterThanOrEqualTo("timestamp", startMs)
                .get()
                .addOnSuccessListener(snapshot -> {

                    Map<String, Integer> dosesPerDate = new HashMap<>();

                    for (QueryDocumentSnapshot logDoc : snapshot) {
                        String dateStr = logDoc.getString("date");
                        Long doseCount = logDoc.getLong("doseCount");
                        if (dateStr == null || doseCount == null) continue;

                        int current = dosesPerDate.getOrDefault(dateStr, 0);
                        dosesPerDate.put(dateStr, current + doseCount.intValue());
                    }

                    computeAdherence(plannedWeekdays, start, end, dosesPerDate);
                })
                .addOnFailureListener(e ->
                        textReportAdherence.setText("Could not load controller logs: " + e.getMessage())
                );
    }

    private void computeAdherence(Set<Integer> plannedWeekdays,
                                  Date start,
                                  Date end,
                                  Map<String, Integer> dosesPerDate) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(start);

        int plannedDays = 0;
        int takenDays = 0;

        while (!cal.getTime().after(end)) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            boolean planned = plannedWeekdays.contains(dayOfWeek);

            String dateStr = dateFormat.format(cal.getTime());
            boolean taken = dosesPerDate.getOrDefault(dateStr, 0) > 0;

            if (planned) {
                plannedDays++;
                if (taken) takenDays++;
            }

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (plannedDays == 0) {
            textReportAdherence.setText("Controller schedule has no planned days in the last 30 days.");
            return;
        }

        double pct = takenDays * 100.0 / plannedDays;
        String text = "Controller adherence (last 30 days):\n" +
                "- Planned controller days: " + plannedDays + "\n" +
                "- Days with at least one controller dose: " + takenDays + "\n" +
                String.format(Locale.getDefault(), "- Adherence: %.1f%%", pct);

        textReportAdherence.setText(text);
    }
}