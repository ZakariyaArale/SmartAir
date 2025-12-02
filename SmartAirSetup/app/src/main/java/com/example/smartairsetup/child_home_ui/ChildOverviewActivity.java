package com.example.smartairsetup.child_home_ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.Nullable;

import com.example.smartairsetup.navigation.AbstractNavigation;
import com.example.smartairsetup.medlog.ControllerScheduleActivity;
import com.example.smartairsetup.R;
import com.example.smartairsetup.sharing.SymptomTrendActivity;
import com.example.smartairsetup.medlog.MedicationReportActivity;
import com.example.smartairsetup.parent_home_ui.ParentHomeActivity;
import com.example.smartairsetup.pdf.PDFStoreActivity;
import com.example.smartairsetup.sharing.ShareWithProviderActivity;
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
    private TextView textChildSummary;
    private Button buttonSelectChild;
    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();
    private String parentUid;
    private String selectedChildId;
    private String selectedChildName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textChildSummary = findViewById(R.id.textChildSummary);

        Button buttonOpenSchedule = findViewById(R.id.buttonOpenSchedule);
        Button buttonOpenSymptomTrend = findViewById(R.id.buttonOpenSymptomTrend);
        Button buttonOpenMedicationReport = findViewById(R.id.buttonOpenMedicationReport);
        Button buttonWhatProviderSees = findViewById(R.id.buttonWhatProviderSees);
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        buttonOpenSchedule.setOnClickListener(v -> {
            if (childNotSelected()) return;
            Intent intent = new Intent(ChildOverviewActivity.this, ControllerScheduleActivity.class);
            intent.putExtra(ControllerScheduleActivity.EXTRA_CHILD_ID, selectedChildId);
            intent.putExtra(ControllerScheduleActivity.EXTRA_CHILD_NAME, selectedChildName);
            startActivity(intent);
        });

        buttonOpenSymptomTrend.setOnClickListener(v -> {
            if (childNotSelected()) return;
            Intent intent = new Intent(ChildOverviewActivity.this, SymptomTrendActivity.class);
            intent.putExtra(SymptomTrendActivity.EXTRA_CHILD_ID, selectedChildId);
            intent.putExtra(SymptomTrendActivity.EXTRA_CHILD_NAME, selectedChildName);
            startActivity(intent);
        });

        buttonOpenMedicationReport.setOnClickListener(v -> {
            if (childNotSelected()) return;
            Intent intent = new Intent(ChildOverviewActivity.this, MedicationReportActivity.class);
            intent.putExtra(MedicationReportActivity.EXTRA_CHILD_ID, selectedChildId);
            intent.putExtra(MedicationReportActivity.EXTRA_CHILD_NAME, selectedChildName);
            startActivity(intent);
        });

        buttonWhatProviderSees.setOnClickListener(v -> {
            if (childNotSelected()) return;
            Intent intent = new Intent(ChildOverviewActivity.this, ShareWithProviderActivity.class);
            intent.putExtra(ShareWithProviderActivity.EXTRA_CHILD_ID, selectedChildId);
            intent.putExtra(ShareWithProviderActivity.EXTRA_CHILD_NAME, selectedChildName);
            startActivity(intent);
        });

        buttonOpenMedicationReport.setOnClickListener(v -> {
            if (childNotSelected()) return;
            Intent intent = new Intent(ChildOverviewActivity.this, MedicationReportActivity.class);
            intent.putExtra(MedicationReportActivity.EXTRA_CHILD_ID, selectedChildId);
            intent.putExtra(MedicationReportActivity.EXTRA_CHILD_NAME, selectedChildName);
            startActivity(intent);
        });

        buttonSelectChild = findViewById(R.id.buttonSelectChild);
        textChildSummary = findViewById(R.id.textChildSummary);

        buttonSelectChild.setEnabled(false);
        buttonSelectChild.setOnClickListener(v -> showChildSelectDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildren();
    }

    private boolean childNotSelected() {
        if (selectedChildId == null) {
            Toast.makeText(this, "Please select a child first.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
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

            if (childIds.isEmpty()) {
                selectedChildId = null;
                selectedChildName = null;
                buttonSelectChild.setEnabled(false);
                buttonSelectChild.setText("No children");
                textChildSummary.setText("No children found. Add a child from the Parent Home screen.");
            } else {
                // DO NOT auto-select; require explicit selection
                selectedChildId = null;
                selectedChildName = null;

                buttonSelectChild.setEnabled(true);
                buttonSelectChild.setText("Select child");
                textChildSummary.setText("Select a child to view their summary.");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load children: " + e.getMessage(), Toast.LENGTH_LONG).show();
            buttonSelectChild.setEnabled(false);
            buttonSelectChild.setText("Error loading");
        });
    }

    private void showChildSelectDialog() {
        if (childIds.isEmpty()) {
            Toast.makeText(this, "Please add a child first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] namesArray = childNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Select a child")
                .setItems(namesArray, (dialog, which) -> {
                    if (which >= 0 && which < childIds.size()) {
                        selectedChildId = childIds.get(which);
                        selectedChildName = childNames.get(which);
                        buttonSelectChild.setText(selectedChildName);
                        loadOverviewForChild(selectedChildId, selectedChildName);
                    }
                })
                .show();
    }

    @SuppressLint("SetTextI18n")
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
    @androidx.annotation.LayoutRes
    protected int getLayoutResourceId() {
        return R.layout.activity_child_overview;
    }

    @Override
    protected void onHomeClicked() {
        // Parent home (the shortcuts and child summary page)
        Intent intent = new Intent(this, ParentHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onFamilyClicked() {
        // Where “family/children” nav goes in the rest of the app.
        // If your app uses ChildHomeActivity or AddChildActivity, use that one:
        Intent intent = new Intent(this, ChildHomeActivity.class);
        // or: new Intent(this, AddChildActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        // TODO: Parent settings screen if you create one later
    }

    @Override
    protected void onEmergencyClicked() {
        // Emergency plan / PDF
        Intent intent = new Intent(this, PDFStoreActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }
}