package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ParentHomeActivity extends AbstractNavigation {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button buttonAddChild;
    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();

    // Child overview UI
    private String parentUid;
    private TextView textWeeklyRescue;
    private Button buttonOverviewSelectChild;
    private String selectedOverviewChildId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Button buttonViewHistory = findViewById(R.id.buttonViewHistory);
        buttonViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        Button buttonChildOverview = findViewById(R.id.buttonChildOverview);
        buttonChildOverview.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, ChildOverviewActivity.class);
            startActivity(intent);
        });

        Button buttonSetPB = findViewById(R.id.buttonSetPB);
        buttonSetPB.setOnClickListener(v -> {
            if (childIds.isEmpty()) {
                Toast.makeText(
                        ParentHomeActivity.this,
                        "Please add a child first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            Intent intent = new Intent(ParentHomeActivity.this, PBActivity.class);
            startActivity(intent);
        });

        Button buttonEnterPEF = findViewById(R.id.buttonEnterPEF);
        buttonEnterPEF.setOnClickListener(v -> {
            if (childIds.isEmpty()) {
                Toast.makeText(
                        ParentHomeActivity.this,
                        "Please add a child first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            Intent intent = new Intent(ParentHomeActivity.this, PEFActivity.class);
            startActivity(intent);
        });

        Button buttonAddBadges = findViewById(R.id.buttonAddBadges);
        buttonAddBadges.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, ParentBadgeSettingsActivity.class);
            startActivity(intent);
        });

        Button buttonDailyCheckIn = findViewById(R.id.buttonDailyCheckIn);
        buttonDailyCheckIn.setOnClickListener(v -> {
            if (childIds.isEmpty()) {
                Toast.makeText(
                        ParentHomeActivity.this,
                        "Please add a child first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (childIds.size() == 1) {
                launchDailyCheckIn(childIds.get(0), childNames.get(0));
            } else {
                String[] namesArray = childNames.toArray(new String[0]);

                new AlertDialog.Builder(ParentHomeActivity.this)
                        .setTitle("Select a child")
                        .setItems(namesArray, (dialog, which) -> {
                            if (which >= 0 && which < childIds.size()) {
                                String childId = childIds.get(which);
                                String childName = childNames.get(which);
                                launchDailyCheckIn(childId, childName);
                            }
                        })
                        .show();
            }
        });

        buttonAddChild = findViewById(R.id.buttonAddChild);
        buttonAddChild.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, AddChildActivity.class);
            startActivity(intent);
        });

        // Child summary card (button + weekly text)
        buttonOverviewSelectChild = findViewById(R.id.buttonOverviewSelectChild);
        textWeeklyRescue = findViewById(R.id.textWeeklyRescue);

        // Disable until children are loaded
        buttonOverviewSelectChild.setEnabled(false);
        buttonOverviewSelectChild.setOnClickListener(v -> showOverviewChildDialog());

        loadChildren();

        Button buttonMedicatitonInventory = findViewById(R.id.buttonMedicationInventory);
        buttonMedicatitonInventory.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, MedicationInventoryActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildren();
    }

    private void loadChildren() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentUidLocal = mAuth.getCurrentUser().getUid();
        this.parentUid = parentUidLocal;

        CollectionReference childrenRef = db.collection("users")
                .document(parentUidLocal)
                .collection("children");

        childrenRef.get()
                .addOnSuccessListener(querySnapshot -> {
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
                        // No children for this parent
                        selectedOverviewChildId = null;
                        buttonOverviewSelectChild.setEnabled(false);
                        buttonOverviewSelectChild.setText("No children");
                        textWeeklyRescue.setText("No children found for this parent.");
                    } else {
                        // At least one child – but DO NOT auto-select
                        selectedOverviewChildId = null;

                        buttonOverviewSelectChild.setEnabled(true);
                        buttonOverviewSelectChild.setText("Select child");
                        textWeeklyRescue.setText("Select a child to view this week's rescue count.");
                    }
                })
                .addOnFailureListener(e -> {
                    selectedOverviewChildId = null;
                    buttonOverviewSelectChild.setEnabled(false);
                    buttonOverviewSelectChild.setText("No children");
                    textWeeklyRescue.setText("Error loading children.");
                    Toast.makeText(this, "Failed to load children: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showOverviewChildDialog() {
        if (childIds.isEmpty()) {
            Toast.makeText(this, "Please add a child first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] namesArray = childNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Select a child")
                .setItems(namesArray, (dialog, which) -> {
                    if (which >= 0 && which < childIds.size()) {
                        selectedOverviewChildId = childIds.get(which);
                        String name = childNames.get(which);
                        buttonOverviewSelectChild.setText(name);
                        loadWeeklyRescueForChild(selectedOverviewChildId);
                    }
                })
                .show();
    }

    private void loadWeeklyRescueForChild(String childId) {
        if (parentUid == null || childId == null) {
            textWeeklyRescue.setText("Could not load weekly rescue data.");
            return;
        }

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (!doc.exists()) {
                        textWeeklyRescue.setText("No weekly rescue data for this child.");
                        return;
                    }

                    Long countLong = doc.getLong("weekly_rescue_medication_count");
                    int count = 0;
                    if (countLong != null) {
                        count = countLong.intValue();
                    }

                    textWeeklyRescue.setText(
                            "Rescue medication count this week: " + count
                    );
                })
                .addOnFailureListener(e ->
                        textWeeklyRescue.setText("Could not load weekly rescue data.")
                );
    }

    private void launchDailyCheckIn(String childId, String childName) {
        Intent intent = new Intent(ParentHomeActivity.this, DailyCheckIn.class);
        intent.putExtra(DailyCheckIn.EXTRA_CHILD_ID, childId);
        intent.putExtra(DailyCheckIn.EXTRA_CHILD_NAME, childName);
        startActivity(intent);
    }

    // --- Navigation integration from your code ---

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_parent_home;
    }

    @Override
    protected void onHomeClicked() {
        // Already on home
    }

    @Override
    protected void onFamilyClicked() {
        Intent intent = new Intent(ParentHomeActivity.this, ParentFamilyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onEmergencyClicked() {
        Intent intent = new Intent(this, EmergencyActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        // TODO: For a Parent, this would go to ParentSettingsActivity
        // Intent intent = new Intent(this, ParentSettingsActivity.class);
        // startActivity(intent);
    }
}
