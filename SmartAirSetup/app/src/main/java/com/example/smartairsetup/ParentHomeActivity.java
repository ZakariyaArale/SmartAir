package com.example.smartairsetup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.AdapterView;

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

    private ListView listChildren;
    private TextView textNoChildren;
    private Button buttonAddChild;

    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();
    private ArrayAdapter<String> childrenAdapter;

    // Child overview UI
    private Spinner spinnerChildSelector;
    private TextView textWeeklyRescue;
    private ArrayAdapter<String> childSelectorAdapter;
    private String parentUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Button buttonViewHistory = findViewById(R.id.buttonViewHistory);
        buttonViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, HistoryActivity.class);
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

        listChildren = findViewById(R.id.listChildren);
        textNoChildren = findViewById(R.id.textNoChildren);
        buttonAddChild = findViewById(R.id.buttonAddChild);

        childrenAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                childNames
        );
        listChildren.setAdapter(childrenAdapter);

        buttonAddChild.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, AddChildActivity.class);
            startActivity(intent);
        });

        listChildren.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= childIds.size()) {
                return;
            }

            String childId = childIds.get(position);
            String childName = childNames.get(position);

            Intent intent = new Intent(ParentHomeActivity.this, ShareWithProviderActivity.class);
            intent.putExtra(ShareWithProviderActivity.EXTRA_CHILD_ID, childId);
            intent.putExtra(ShareWithProviderActivity.EXTRA_CHILD_NAME, childName);
            startActivity(intent);
        });

        // Child overview card (spinner + weekly text)
        spinnerChildSelector = findViewById(R.id.spinnerChildSelector);
        textWeeklyRescue = findViewById(R.id.textWeeklyRescue);

        childSelectorAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                childNames
        );
        childSelectorAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerChildSelector.setAdapter(childSelectorAdapter);

        spinnerChildSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id
            ) {
                if (position >= 0 && position < childIds.size()) {
                    String childId = childIds.get(position);
                    loadWeeklyRescueForChild(childId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                textWeeklyRescue.setText(
                        "Select a child to see this week's rescue medication count."
                );
            }
        });

        loadChildren();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildren();
    }

    private void loadChildren() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
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

                    childrenAdapter.notifyDataSetChanged();
                    childSelectorAdapter.notifyDataSetChanged();

                    if (childIds.isEmpty()) {
                        textNoChildren.setVisibility(View.VISIBLE);
                        listChildren.setVisibility(View.GONE);

                        spinnerChildSelector.setEnabled(false);
                        textWeeklyRescue.setText("No children found for this parent.");
                    } else {
                        textNoChildren.setVisibility(View.GONE);
                        listChildren.setVisibility(View.VISIBLE);

                        spinnerChildSelector.setEnabled(true);
                        spinnerChildSelector.setSelection(0);

                        String firstChildId = childIds.get(0);
                        loadWeeklyRescueForChild(firstChildId);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load children: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
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
        Intent intent = new Intent(ParentHomeActivity.this, AddChildActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onProfileClicked() {
        // TODO: For a Parent, this would go to ParentProfileActivity
    }

    @Override
    protected void onSettingsClicked() {
        // TODO: For a Parent, this would go to ParentSettingsActivity
    }
}
