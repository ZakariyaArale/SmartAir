package com.example.smartairsetup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public class ShareWithProviderActivity extends AppCompatActivity {

    public static final String EXTRA_CHILD_ID = "extra_child_id";
    public static final String EXTRA_CHILD_NAME = "extra_child_name";

    private Switch switchRescueLogs, switchControllerSummary, switchSymptoms, switchTriggers,
            switchPEF, switchTriageIncidents, switchSummaryCharts;

    private EditText editProviderEmail;
    private Button buttonAddProvider;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference childDocRef;

    private boolean updatingFromBackend = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_with_provider);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

        TextView textChildName = findViewById(R.id.textChildName);
        if (childName != null) textChildName.setText(childName);

        switchRescueLogs = findViewById(R.id.switchRescueLogs);
        switchControllerSummary = findViewById(R.id.switchControllerSummary);
        switchSymptoms = findViewById(R.id.switchSymptoms);
        switchTriggers = findViewById(R.id.switchTriggers);
        switchPEF = findViewById(R.id.switchPEF);
        switchTriageIncidents = findViewById(R.id.switchTriageIncidents);
        switchSummaryCharts = findViewById(R.id.switchSummaryCharts);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // NEW: provider binding
        editProviderEmail = findViewById(R.id.editProviderEmail);
        buttonAddProvider = findViewById(R.id.buttonAddProvider);

        buttonAddProvider.setOnClickListener(v -> addProviderByEmail());

        CompoundButton.OnCheckedChangeListener toggleListener = (button, isChecked) -> {
            if (updatingFromBackend) return;

            String field = null;
            int id = button.getId();
            if (id == R.id.switchRescueLogs) field = "shareRescueLogs";
            else if (id == R.id.switchControllerSummary) field = "shareControllerSummary";
            else if (id == R.id.switchSymptoms) field = "shareSymptoms";
            else if (id == R.id.switchTriggers) field = "shareTriggers";
            else if (id == R.id.switchPEF) field = "sharePEF";
            else if (id == R.id.switchTriageIncidents) field = "shareTriageIncidents";
            else if (id == R.id.switchSummaryCharts) field = "shareSummaryCharts";

            if (field != null) {
                childDocRef.update(field, isChecked)
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to update sharing: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show()
                        );
            }
        };

        switchRescueLogs.setOnCheckedChangeListener(toggleListener);
        switchControllerSummary.setOnCheckedChangeListener(toggleListener);
        switchSymptoms.setOnCheckedChangeListener(toggleListener);
        switchTriggers.setOnCheckedChangeListener(toggleListener);
        switchPEF.setOnCheckedChangeListener(toggleListener);
        switchTriageIncidents.setOnCheckedChangeListener(toggleListener);
        switchSummaryCharts.setOnCheckedChangeListener(toggleListener);

        // Sync UI from Firestore
        childDocRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;

            updatingFromBackend = true;

            switchRescueLogs.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("shareRescueLogs")));
            switchControllerSummary.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("shareControllerSummary")));
            switchSymptoms.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("shareSymptoms")));
            switchTriggers.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("shareTriggers")));
            switchPEF.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("sharePEF")));
            switchTriageIncidents.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("shareTriageIncidents")));
            switchSummaryCharts.setChecked(Boolean.TRUE.equals(snapshot.getBoolean("shareSummaryCharts")));

            updatingFromBackend = false;
        });
    }

    private void addProviderByEmail() {
        String email = editProviderEmail.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(email)) {
            editProviderEmail.setError("Enter provider email");
            editProviderEmail.requestFocus();
            return;
        }

        buttonAddProvider.setEnabled(false);

        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", "provider")
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        Toast.makeText(this, "No provider found with that email.", Toast.LENGTH_SHORT).show();
                        buttonAddProvider.setEnabled(true);
                        return;
                    }

                    String providerUid = qs.getDocuments().get(0).getId();

                    childDocRef.update("sharedProviderUids", FieldValue.arrayUnion(providerUid))
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Provider added for this child.", Toast.LENGTH_SHORT).show();
                                editProviderEmail.setText("");
                                buttonAddProvider.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to add provider: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                buttonAddProvider.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lookup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonAddProvider.setEnabled(true);
                });
    }
}