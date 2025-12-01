package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentBadgeSettingsActivity extends AppCompatActivity {

    // UI
    private Spinner spinnerChildren;
    private Button backButton;
    private EditText editTechniqueSessions;
    private EditText editLowRescueDays;
    private Button buttonSaveBadgeSettings;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Spinner data
    private ArrayAdapter<String> childrenAdapter;
    private final List<String> childNames = new ArrayList<String>();
    private final List<String> childIds = new ArrayList<String>();

    private String selectedChildId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_badge_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        spinnerChildren = findViewById(R.id.spinnerChildren);
        backButton = findViewById(R.id.backButton);
        editTechniqueSessions = findViewById(R.id.editTechniqueSessions);
        editLowRescueDays = findViewById(R.id.editLowRescueDays);
        buttonSaveBadgeSettings = findViewById(R.id.buttonSaveBadgeSettings);

        setupBackButton();
        setupChildrenSpinner();
        setupSaveButton();

        loadChildrenForParent();
    }

    // ------------------------------
    // UI Setup
    // ------------------------------

    private void setupBackButton() {
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(
                    ParentBadgeSettingsActivity.this,
                    ParentHomeActivity.class
            );
            startActivity(intent);
            finish();
        });
    }

    private void setupChildrenSpinner() {
        childrenAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                childNames
        );
        childrenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChildren.setAdapter(childrenAdapter);

        spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parentView,
                    View view,
                    int position,
                    long id
            ) {
                if (position >= 0 && position < childIds.size()) {
                    selectedChildId = childIds.get(position);
                    loadBadgeSettingsForChild(selectedChildId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                selectedChildId = null;
            }
        });
    }

    private void setupSaveButton() {
        buttonSaveBadgeSettings.setOnClickListener(view -> saveBadgeSettingsForSelectedChild());
    }

    // ------------------------------
    // Firestore: Load children for this parent
    // ------------------------------

    private void loadChildrenForParent() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be signed in as a parent.", Toast.LENGTH_SHORT).show();
            spinnerChildren.setEnabled(false);
            return;
        }

        String parentUid = currentUser.getUid();

        CollectionReference childrenRef = db.collection("users")
                .document(parentUid)
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

                    if (childIds.isEmpty()) {
                        spinnerChildren.setEnabled(false);
                        Toast.makeText(
                                ParentBadgeSettingsActivity.this,
                                "No children found for this parent.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        spinnerChildren.setEnabled(true);
                        selectedChildId = childIds.get(0);
                        spinnerChildren.setSelection(0);
                        loadBadgeSettingsForChild(selectedChildId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            ParentBadgeSettingsActivity.this,
                            "Failed to load children: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    spinnerChildren.setEnabled(false);
                });
    }

    // ------------------------------
    // Firestore: Badge settings per child
    // ------------------------------

    private void loadBadgeSettingsForChild(String childId) {
        if (TextUtils.isEmpty(childId)) {
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be signed in as a parent.", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentUid = currentUser.getUid();

        CollectionReference badgesRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("badges");

        badgesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(
                    @NonNull Task<QuerySnapshot> task
            ) {
                if (!task.isSuccessful()) {
                    Toast.makeText(
                            ParentBadgeSettingsActivity.this,
                            "Failed to load badge goals.",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                int techniqueTarget = 10;      // default
                int lowRescueThreshold = 4;    // default

                for (DocumentSnapshot doc : task.getResult()) {
                    String badgeId = doc.getId();
                    Long targetLong = doc.getLong("target");

                    if (targetLong == null) {
                        continue;
                    }

                    int target = targetLong.intValue();

                    if ("technique_sessions".equals(badgeId)) {
                        techniqueTarget = target;
                    } else if ("low_rescue_month".equals(badgeId)) {
                        lowRescueThreshold = target;
                    }
                }

                editTechniqueSessions.setText(String.valueOf(techniqueTarget));
                editLowRescueDays.setText(String.valueOf(lowRescueThreshold));
            }
        });
    }

    private void saveBadgeSettingsForSelectedChild() {
        if (TextUtils.isEmpty(selectedChildId)) {
            Toast.makeText(this, "Please select a child.", Toast.LENGTH_SHORT).show();
            return;
        }

        String techniqueText = editTechniqueSessions.getText().toString().trim();
        String lowRescueText = editLowRescueDays.getText().toString().trim();

        if (TextUtils.isEmpty(techniqueText)) {
            editTechniqueSessions.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(lowRescueText)) {
            editLowRescueDays.setError("Required");
            return;
        }

        int techniqueTarget;
        int lowRescueThreshold;

        try {
            techniqueTarget = Integer.parseInt(techniqueText);
            lowRescueThreshold = Integer.parseInt(lowRescueText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (techniqueTarget <= 0 || lowRescueThreshold < 0) {
            Toast.makeText(
                    this,
                    "Values must be positive (rescue threshold can be 0 or more).",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be signed in as a parent.", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentUid = currentUser.getUid();

        CollectionReference badgesRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(selectedChildId)
                .collection("badges");

        Map<String, Object> techniqueData = new HashMap<String, Object>();
        techniqueData.put("target", techniqueTarget);

        Map<String, Object> lowRescueData = new HashMap<String, Object>();
        lowRescueData.put("target", lowRescueThreshold);

        // Save both documents; show a single toast based on low_rescue_month write
        badgesRef.document("technique_sessions").set(techniqueData);
        badgesRef.document("low_rescue_month").set(lowRescueData)
                .addOnSuccessListener(unused -> Toast.makeText(
                                ParentBadgeSettingsActivity.this,
                                "Badge goals saved for this child.",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(e -> Toast.makeText(
                                ParentBadgeSettingsActivity.this,
                                "Failed to save badge goals.",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}
