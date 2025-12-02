package com.example.smartairsetup.provider_home_ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.example.smartairsetup.history.HistoryEntry;
import com.example.smartairsetup.history.HistoryEntryAdapter;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class ProviderSymptomsActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private final List<HistoryEntry> items = new ArrayList<>();
    private HistoryEntryAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_symptoms);

        db = FirebaseFirestore.getInstance();

        String parentUid = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_PARENT_UID);
        String childId = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_CHILD_ID);
        String childName = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_CHILD_NAME);

        if (parentUid == null || childId == null) {
            Toast.makeText(this, "Missing child info", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView title = findViewById(R.id.textTitle);
        title.setText((childName != null ? childName : "Child") + " • Symptoms");

        ListView list = findViewById(R.id.listSymptoms);
        TextView summary = findViewById(R.id.textSummary);

        adapter = new HistoryEntryAdapter(this, items);
        list.setAdapter(adapter);

        // gate by shareSymptoms (so provider can’t see if parent toggled off)
        DocumentReference childRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId);

        childRef.get().addOnSuccessListener(snap -> {
            boolean shareSymptoms = snap.exists() && Boolean.TRUE.equals(snap.getBoolean("shareSymptoms"));
            if (!shareSymptoms) {
                Toast.makeText(this, "Symptoms are not shared for this child.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            loadSymptoms(parentUid, childId, summary);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to check sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        });

        Button back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());
    }

    private void loadSymptoms(String parentUid, String childId, TextView summaryTv) {
        // Same data source as HistoryActivity, but provider reads via parentUid
        CollectionReference ref = db.collection("users")
                .document(parentUid)
                .collection("dailyCheckins");

        // Filter to this child
        ref.whereEqualTo("childId", childId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    items.clear();

                    int nightProblemDays = 0;
                    int activityProblemDays = 0;
                    int coughProblemDays = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String date = doc.getString("date");

                        String night = safe(doc.getString("nightWaking"));
                        String activity = safe(doc.getString("activityLimits"));
                        String cough = safe(doc.getString("coughWheeze"));
                        @SuppressWarnings("unchecked")
                        List<String> triggers = (List<String>) doc.get("triggers");
                        String authorLabel = safe(doc.getString("authorLabel"));

                        if (!"none".equalsIgnoreCase(night)) nightProblemDays++;
                        if (!"none".equalsIgnoreCase(activity)) activityProblemDays++;
                        if (!"none".equalsIgnoreCase(cough)) coughProblemDays++;

                        String triggersDisplay = (triggers == null || triggers.isEmpty())
                                ? "none"
                                : TextUtils.join(", ", triggers);

                        items.add(new HistoryEntry(
                                "Symptoms",                       // childName column in adapter
                                date != null ? date : "Unknown",  // date
                                night,                            // “Night:”
                                activity,                         // “Activity:”
                                cough,                            // “Cough/Wheeze:”
                                triggersDisplay,                  // triggers
                                authorLabel.toLowerCase()         // author
                        ));
                    }

                    adapter.notifyDataSetChanged();

                    summaryTv.setText(
                            "Days with night waking: " + nightProblemDays + "\n" +
                                    "Days with activity limits: " + activityProblemDays + "\n" +
                                    "Days with cough/wheeze: " + coughProblemDays
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load symptoms: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "n/a" : s.trim();
    }
}