package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChildOverviewActivity extends AbstractNavigation {

    public static final String EXTRA_CHILD_ID = "extra_child_id";
    public static final String EXTRA_CHILD_NAME = "extra_child_name";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Spinner spinnerChildSelector;
    private TextView textChildSummary;

    private Button buttonOpenSchedule;
    private Button buttonOpenSymptomTrend;
    private Button buttonOpenMedicationReport;
    private Button buttonWhatProviderSees;

    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();
    private ArrayAdapter<String> childAdapter;

    private String parentUid;
    private String selectedChildId;
    private String selectedChildName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        spinnerChildSelector = findViewById(R.id.spinnerChildSelector);
        textChildSummary = findViewById(R.id.textChildSummary);

        buttonOpenSchedule = findViewById(R.id.buttonOpenSchedule);
        buttonOpenSymptomTrend = findViewById(R.id.buttonOpenSymptomTrend);
        buttonOpenMedicationReport = findViewById(R.id.buttonOpenMedicationReport);
        buttonWhatProviderSees = findViewById(R.id.buttonWhatProviderSees);

        childAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                childNames
        );
        childAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChildSelector.setAdapter(childAdapter);

        spinnerChildSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id
            ) {
                if (position >= 0 && position < childIds.size()) {
                    selectedChildId = childIds.get(position);
                    selectedChildName = childNames.get(position);
                    loadOverviewForChild(selectedChildId, selectedChildName);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });

        buttonOpenSchedule.setOnClickListener(v -> {
            if (!ensureChildSelected()) return;
            Intent intent = new Intent(ChildOverviewActivity.this, ControllerScheduleActivity.class);
            intent.putExtra(ControllerScheduleActivity.EXTRA_CHILD_ID, selectedChildId);
            intent.putExtra(ControllerScheduleActivity.EXTRA_CHILD_NAME, selectedChildName);
            startActivity(intent);
        });

        buttonOpenSymptomTrend.setOnClickListener(v -> {
            if (!ensureChildSelected()) return;
            Intent intent = new Intent(ChildOverviewActivity.this, SymptomTrendActivity.class);
            intent.putExtra(SymptomTrendActivity.EXTRA_CHILD_ID, selectedChildId);
            intent.putExtra(SymptomTrendActivity.EXTRA_CHILD_NAME, selectedChildName);
            startActivity(intent);
        });

        buttonOpenMedicationReport.setOnClickListener(v -> {
            if (!ensureChildSelected()) return;
            Intent intent = new Intent(ChildOverviewActivity.this, MedicationReportActivity.class);
            intent.putExtra(MedicationReportActivity.EXTRA_CHILD_ID, selectedChildId);
            intent.putExtra(MedicationReportActivity.EXTRA_CHILD_NAME, selectedChildName);
            startActivity(intent);
        });

        buttonWhatProviderSees.setOnClickListener(v -> {
            if (!ensureChildSelected()) return;
            Intent intent = new Intent(ChildOverviewActivity.this, ShareWithProviderActivity.class);
            intent.putExtra(ShareWithProviderActivity.EXTRA_CHILD_ID, selectedChildId);
            intent.putExtra(ShareWithProviderActivity.EXTRA_CHILD_NAME, selectedChildName);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildren();
    }

    private boolean ensureChildSelected() {
        if (selectedChildId == null) {
            Toast.makeText(this, "Please select a child first.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void loadChildren() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        parentUid = mAuth.getCurrentUser().getUid();

        CollectionReference childrenRef = db.collection("users")
                .document(parentUid)
                .collection("children");

        childrenRef.get().addOnSuccessListener(querySnapshot -> {
            childNames.clear();
            childIds.clear();

            for (QueryDocumentSnapshot doc : querySnapshot) {
                String name = doc.getString("name");
                if (name == null || name.trim().isEmpty()) {
                    name = "(Unnamed child)";
                }
                childNames.add(name);
                childIds.add(doc.getId());
            }

            childAdapter.notifyDataSetChanged();

            if (childIds.isEmpty()) {
                spinnerChildSelector.setEnabled(false);
                textChildSummary.setText("No children found. Add a child from the Parent Home screen.");
                selectedChildId = null;
                selectedChildName = null;
            } else {
                spinnerChildSelector.setEnabled(true);
                spinnerChildSelector.setSelection(0);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load children: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void loadOverviewForChild(String childId, String childName) {
        // For now, we’ll show weekly rescue count as a simple summary
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    Long weeklyRescue = doc.getLong("weekly_rescue_medication_count");
                    int rescueCount = weeklyRescue == null ? 0 : weeklyRescue.intValue();

                    String summary = "Child: " + childName +
                            "\nRescue medication count (this week): " + rescueCount;

                    textChildSummary.setText(summary);
                })
                .addOnFailureListener(e ->
                        textChildSummary.setText("Could not load summary: " + e.getMessage())
                );
    }

    // ----- Navigation hooks -----

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_child_overview;
    }

    @Override
    protected void onHomeClicked() {
        Intent intent = new Intent(this, ParentHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onFamilyClicked() {
        Intent intent = new Intent(this, AddChildActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onProfileClicked() {
        // TODO: Parent profile if needed
    }

    @Override
    protected void onSettingsClicked() {
        // TODO: Parent settings if needed
    }
}