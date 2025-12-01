package com.example.smartairsetup.medlog;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.example.smartairsetup.parent_home_ui.ParentHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ControllerLogActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Spinner spinnerChildFilter;
    private Spinner spinnerMedTypeFilter;
    private EditText editStartDate;
    private EditText editEndDate;
    private Button buttonApplyFilters;
    private ListView listHistory;

    private final List<String> childIds = new ArrayList<>();
    private final List<String> childNames = new ArrayList<>();

    private final List<MedicationLogEntry> logItems = new ArrayList<>();
    private MedicationLogAdapter logAdapter;

    private final SimpleDateFormat dateOnlyFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller_log);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextView title = findViewById(R.id.textControllerLogTitle);
        title.setText("Medication Logs");

        spinnerChildFilter = findViewById(R.id.spinnerChildFilter);
        spinnerMedTypeFilter = findViewById(R.id.spinnerTriggerFilter);
        editStartDate = findViewById(R.id.editStartDate);
        editEndDate = findViewById(R.id.editEndDate);
        buttonApplyFilters = findViewById(R.id.buttonApplyFilters);
        listHistory = findViewById(R.id.listHistory);

        setupMedTypeSpinner();
        loadChildrenForFilter();

        logAdapter = new MedicationLogAdapter(this, logItems);
        listHistory.setAdapter(logAdapter);

        buttonApplyFilters.setOnClickListener(v -> loadLogs());
        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> goBack());
    }
    private void goBack(){
        Intent intent = new Intent(ControllerLogActivity.this, ParentHomeActivity.class);
        startActivity(intent);
    }
    private void setupMedTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.med_type_filter_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedTypeFilter.setAdapter(adapter);
    }

    private void loadChildrenForFilter() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        CollectionReference childrenRef = db.collection("users")
                .document(uid)
                .collection("children");

        childrenRef.get().addOnSuccessListener(snapshot -> {
            childIds.clear();
            childNames.clear();

            childIds.add(null);
            childNames.add("Select child");

            for (QueryDocumentSnapshot childDoc : snapshot) {
                String name = childDoc.getString("name");
                if (name == null || name.trim().isEmpty()) {
                    name = "Unnamed child";
                }
                childIds.add(childDoc.getId());
                childNames.add(name);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    childNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerChildFilter.setAdapter(adapter);
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load children: " + e.getMessage(),
                        Toast.LENGTH_LONG).show()
        );
    }

    private void loadLogs() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        int childPos = spinnerChildFilter.getSelectedItemPosition();
        if (childPos <= 0 || childPos >= childIds.size()) {
            Toast.makeText(this, "Please select a child.", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedChildId = childIds.get(childPos);

        String startStr = editStartDate.getText().toString().trim();
        String endStr = editEndDate.getText().toString().trim();

        boolean hasDateRange = !TextUtils.isEmpty(startStr) && !TextUtils.isEmpty(endStr);

        long startMs = 0L;
        long endMs = 0L;

        if (hasDateRange) {
            try {
                Date startDate = dateOnlyFormat.parse(startStr);
                Date endDate = dateOnlyFormat.parse(endStr);
                if (startDate == null || endDate == null) {
                    Toast.makeText(this, "Invalid date format.", Toast.LENGTH_LONG).show();
                    return;
                }
                startMs = startDate.getTime();
                long oneDay = 24L * 60L * 60L * 1000L;
                endMs = endDate.getTime() + oneDay;
            } catch (ParseException e) {
                Toast.makeText(this, "Invalid date format. Use YYYY-MM-DD.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        int medTypePos = spinnerMedTypeFilter.getSelectedItemPosition();
        // 0: All, 1: Rescue only, 2: Controller only

        final long finalStartMs = startMs;
        final long finalEndMs = endMs;
        final boolean finalHasDateRange = hasDateRange;
        final int finalMedTypePos = medTypePos;
        final int finalChildIndex = childPos;

        CollectionReference medsRef = db.collection("users")
                .document(uid)
                .collection("children")
                .document(selectedChildId)
                .collection("medications");

        medsRef.get().addOnSuccessListener(medsSnapshot -> {

            Map<String, String> medNameById = new HashMap<>();

            for (QueryDocumentSnapshot medDoc : medsSnapshot) {
                String medDocId = medDoc.getId();
                String medName = medDoc.getString("name");
                if (medName == null || medName.trim().isEmpty()) {
                    medName = "Medication";
                }
                medNameById.put(medDocId, medName);
            }

            CollectionReference medLogsRef = db.collection("users")
                    .document(uid)
                    .collection("children")
                    .document(selectedChildId)
                    .collection("medLogs");

            medLogsRef
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        logItems.clear();

                        for (QueryDocumentSnapshot doc : snapshot) {

                            Long ts = doc.getLong("timestamp");
                            if (ts == null) {
                                continue;
                            }

                            if (finalHasDateRange) {
                                if (ts < finalStartMs || ts > finalEndMs) {
                                    continue;
                                }
                            }

                            Boolean isRescue = doc.getBoolean("isRescue");
                            if (isRescue == null) {
                                isRescue = false;
                            }

                            if (finalMedTypePos == 1 && !isRescue) {
                                continue;
                            }
                            if (finalMedTypePos == 2 && isRescue) {
                                continue;
                            }

                            Long doseCountLong = doc.getLong("doseCount");
                            int doseCount = 0;
                            if (doseCountLong != null) {
                                doseCount = doseCountLong.intValue();
                            }

                            String typeLabel;
                            if (isRescue) {
                                typeLabel = "Rescue medication";
                            } else {
                                typeLabel = "Controller medication";
                            }

                            String medId = doc.getString("medId");
                            String medName = null;
                            if (medId != null) {
                                medName = medNameById.get(medId);
                            }
                            if (medName == null) {
                                medName = typeLabel;
                            }

                            String timeString = dateTimeFormat.format(new Date(ts));
                            String childName = childNames.get(finalChildIndex);

                            MedicationLogEntry entry = new MedicationLogEntry(
                                    childName,
                                    medName,
                                    timeString,
                                    typeLabel,
                                    doseCount,
                                    null,
                                    null,
                                    null
                            );

                            logItems.add(entry);
                        }

                        logAdapter.notifyDataSetChanged();

                        if (logItems.isEmpty()) {
                            Toast.makeText(this, "No logs found for the selected filters.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load logs: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load medications: " + e.getMessage(),
                        Toast.LENGTH_LONG).show()
        );
    }
}
