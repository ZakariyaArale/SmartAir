package com.example.smartairsetup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ShareWithProviderActivity extends AppCompatActivity {

    public static final String EXTRA_CHILD_ID = "extra_child_id";
    public static final String EXTRA_CHILD_NAME = "extra_child_name";

    private Switch switchRescueLogs;
    private Switch switchControllerSummary;
    private Switch switchSymptoms;
    private Switch switchTriggers;
    private Switch switchPEF;
    private Switch switchTriageIncidents;
    private Switch switchSummaryCharts;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference childDocRef;

    // Guard flag so we don't trigger updates when we are just syncing from Firestore
    private boolean updatingFromBackend = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_with_provider);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get child info from Intent
        String childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        String childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);

        if (mAuth.getCurrentUser() == null || childId == null) {
            Toast.makeText(this, "Missing parent or child information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String parentUid = mAuth.getCurrentUser().getUid();
        childDocRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId);

        // Bind views
        TextView textChildName = findViewById(R.id.textChildName);
        if (childName != null) {
            textChildName.setText(childName);
        }

        switchRescueLogs = findViewById(R.id.switchRescueLogs);
        switchControllerSummary = findViewById(R.id.switchControllerSummary);
        switchSymptoms = findViewById(R.id.switchSymptoms);
        switchTriggers = findViewById(R.id.switchTriggers);
        switchPEF = findViewById(R.id.switchPEF);
        switchTriageIncidents = findViewById(R.id.switchTriageIncidents);
        switchSummaryCharts = findViewById(R.id.switchSummaryCharts);
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // One listener for all switches -> update the corresponding Firestore field
        CompoundButton.OnCheckedChangeListener toggleListener = (button, isChecked) -> {
            if (updatingFromBackend) {
                // We're just syncing UI from Firestore; don't write back
                return;
            }

            String field = null;
            int id = button.getId();
            if (id == R.id.switchRescueLogs) {
                field = "shareRescueLogs";
            } else if (id == R.id.switchControllerSummary) {
                field = "shareControllerSummary";
            } else if (id == R.id.switchSymptoms) {
                field = "shareSymptoms";
            } else if (id == R.id.switchTriggers) {
                field = "shareTriggers";
            } else if (id == R.id.switchPEF) {
                field = "sharePEF";
            } else if (id == R.id.switchTriageIncidents) {
                field = "shareTriageIncidents";
            } else if (id == R.id.switchSummaryCharts) {
                field = "shareSummaryCharts";
            }

            if (field != null) {
                childDocRef.update(field, isChecked)
                        .addOnFailureListener(e ->
                                Toast.makeText(
                                        ShareWithProviderActivity.this,
                                        "Failed to update sharing: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
            }
        };

        // Attach listeners
        switchRescueLogs.setOnCheckedChangeListener(toggleListener);
        switchControllerSummary.setOnCheckedChangeListener(toggleListener);
        switchSymptoms.setOnCheckedChangeListener(toggleListener);
        switchTriggers.setOnCheckedChangeListener(toggleListener);
        switchPEF.setOnCheckedChangeListener(toggleListener);
        switchTriageIncidents.setOnCheckedChangeListener(toggleListener);
        switchSummaryCharts.setOnCheckedChangeListener(toggleListener);

        // Real-time sync from Firestore to UI
        childDocRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                return;
            }

            updatingFromBackend = true;
            Boolean bRescue = snapshot.getBoolean("shareRescueLogs");
            Boolean bController = snapshot.getBoolean("shareControllerSummary");
            Boolean bSymptoms = snapshot.getBoolean("shareSymptoms");
            Boolean bTriggers = snapshot.getBoolean("shareTriggers");
            Boolean bPEF = snapshot.getBoolean("sharePEF");
            Boolean bTriage = snapshot.getBoolean("shareTriageIncidents");
            Boolean bCharts = snapshot.getBoolean("shareSummaryCharts");

            switchRescueLogs.setChecked(Boolean.TRUE.equals(bRescue));
            switchControllerSummary.setChecked(Boolean.TRUE.equals(bController));
            switchSymptoms.setChecked(Boolean.TRUE.equals(bSymptoms));
            switchTriggers.setChecked(Boolean.TRUE.equals(bTriggers));
            switchPEF.setChecked(Boolean.TRUE.equals(bPEF));
            switchTriageIncidents.setChecked(Boolean.TRUE.equals(bTriage));
            switchSummaryCharts.setChecked(Boolean.TRUE.equals(bCharts));

            updatingFromBackend = false;
        });
    }
}
