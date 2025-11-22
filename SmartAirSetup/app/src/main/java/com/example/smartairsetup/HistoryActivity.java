package com.example.smartairsetup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText editStartDate;
    private EditText editEndDate;
    private Spinner spinnerTrigger;
    private Spinner spinnerSymptom;
    private Button buttonApply;
    private ListView listHistory;
    private TextView textSymptomSummary;

    private HistoryEntryAdapter historyAdapter;
    private final List<HistoryEntry> historyItems = new ArrayList<>();

    private Spinner spinnerChild;
    private final List<String> childIds = new ArrayList<>();
    private final List<String> childNames = new ArrayList<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        spinnerChild = findViewById(R.id.spinnerChildFilter);
        loadChildrenForFilter();

        editStartDate = findViewById(R.id.editStartDate);
        editEndDate = findViewById(R.id.editEndDate);
        spinnerTrigger = findViewById(R.id.spinnerTriggerFilter);
        spinnerSymptom = findViewById(R.id.spinnerSymptomFilter);
        buttonApply = findViewById(R.id.buttonApplyFilters);
        listHistory = findViewById(R.id.listHistory);
        textSymptomSummary = findViewById(R.id.textSymptomSummary);

        // Set default date range to last 30 days (or leave empty and handle in code)
        // For now keep empty; you can prefill if you like.

        // Set up spinners from resources
        ArrayAdapter<CharSequence> triggerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.trigger_filter_options,
                android.R.layout.simple_spinner_item
        );
        triggerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrigger.setAdapter(triggerAdapter);

        ArrayAdapter<CharSequence> symptomAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.symptom_filter_options,
                android.R.layout.simple_spinner_item
        );
        symptomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSymptom.setAdapter(symptomAdapter);

        historyAdapter = new HistoryEntryAdapter(this, historyItems);
        listHistory.setAdapter(historyAdapter);

        buttonApply.setOnClickListener(v -> loadHistory());
    }

    private void loadHistory() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        CollectionReference ref = db.collection("users")
                .document(uid)
                .collection("dailyCheckins");

        int childPos = spinnerChild.getSelectedItemPosition();
        String selectedChildId = null;
        if (childPos >= 0 && childPos < childIds.size()) {
            selectedChildId = childIds.get(childPos);  // null means "All children"
        }
        final String filterChildId = selectedChildId;


        String start = editStartDate.getText().toString().trim();
        String end = editEndDate.getText().toString().trim();

        // Basic sanity: if one date is empty, ignore date filtering.
        final boolean hasDateRange = !TextUtils.isEmpty(start) && !TextUtils.isEmpty(end);

        ref.get().addOnSuccessListener(snapshot -> {
            historyItems.clear();

            int nightProblemDays = 0;
            int activityProblemDays = 0;
            int coughProblemDays = 0;

            for (QueryDocumentSnapshot doc : snapshot) {
                String date = doc.getString("date");
                if (hasDateRange && date != null) {
                    // "yyyy-MM-dd" so lexicographic compare works
                    if (date.compareTo(start) < 0 || date.compareTo(end) > 0) {
                        continue;
                    }
                }

                // Child filter
                if (filterChildId != null) {
                    String docChildId = doc.getString("childId");
                    if (docChildId == null || !docChildId.equals(filterChildId)) {
                        continue;
                    }
                }

                // Trigger filter
                if (!passesTriggerFilter(doc)) continue;

                // Symptom filter
                if (!passesSymptomFilter(doc)) continue;

                String night = doc.getString("nightWaking");
                String activity = doc.getString("activityLimits");
                String cough = doc.getString("coughWheeze");
                @SuppressWarnings("unchecked")
                List<String> triggers = (List<String>) doc.get("triggers");
                String authorLabel = doc.getString("authorLabel");
                String docChildName = doc.getString("childName");

                if (night != null && !"none".equals(night)) nightProblemDays++;
                if (activity != null && !"none".equals(activity)) activityProblemDays++;
                if (cough != null && !"none".equals(cough)) coughProblemDays++;

                String triggerText;
                if (triggers == null || triggers.isEmpty()) {
                    triggerText = "Triggers: none";
                } else {
                    // convert ["cold_air", "dust_pets"] -> "cold air, dust/pets"
                    List<String> pretty = new ArrayList<>();
                    for (String t : triggers) {
                        switch (t) {
                            case "cold_air": pretty.add("cold air"); break;
                            case "dust_pets": pretty.add("dust / pets"); break;
                            case "strong_odors": pretty.add("strong odors"); break;
                            default: pretty.add(t.replace('_', ' '));
                        }
                    }
                    triggerText = "Triggers: " + TextUtils.join(", ", pretty);
                }

                String headerChildName = (docChildName != null ? docChildName : "Child");
                String headerDate = (date != null ? date : "Unknown date");

                String nightDisplay = (night != null ? night : "n/a");
                String activityDisplay = (activity != null ? activity : "n/a");
                String coughDisplay = (cough != null ? cough : "n/a");

                String triggersDisplay;
                if (triggers == null || triggers.isEmpty()) {
                    triggersDisplay = "none";
                } else {
                    List<String> pretty = new ArrayList<>();
                    for (String t : triggers) {
                        switch (t) {
                            case "cold_air": pretty.add("cold air"); break;
                            case "dust_pets": pretty.add("dust / pets"); break;
                            case "strong_odors": pretty.add("strong odors"); break;
                            default: pretty.add(t.replace('_', ' '));
                        }
                    }
                    triggersDisplay = TextUtils.join(", ", pretty);
                }

                String authorDisplay = (authorLabel != null ? authorLabel.toLowerCase() : "unknown");

                HistoryEntry entry = new HistoryEntry(
                        headerChildName,
                        headerDate,
                        nightDisplay,
                        activityDisplay,
                        coughDisplay,
                        triggersDisplay,
                        authorDisplay
                );

                historyItems.add(entry);

            }

            historyAdapter.notifyDataSetChanged();

            // Story 16 summary – fill this text (we’ll refine below)
            textSymptomSummary.setText(
                    "Days with night waking: " + nightProblemDays + "\n" +
                            "Days with activity limits: " + activityProblemDays + "\n" +
                            "Days with cough/wheeze: " + coughProblemDays
            );

            // Also load zone change events into the same list
            loadZoneHistory(uid, filterChildId, start, end, hasDateRange);

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load history: " + e.getMessage(),
                        Toast.LENGTH_LONG).show()
        );
    }

    private boolean passesTriggerFilter(QueryDocumentSnapshot doc) {
        int pos = spinnerTrigger.getSelectedItemPosition();
        if (pos == 0) return true; // "Any trigger"

        String requiredKey = null;
        switch (pos) {
            case 1: requiredKey = "exercise"; break;
            case 2: requiredKey = "cold_air"; break;
            case 3: requiredKey = "dust_pets"; break;
            case 4: requiredKey = "smoke"; break;
            case 5: requiredKey = "illness"; break;
            case 6: requiredKey = "strong_odors"; break;
        }

        @SuppressWarnings("unchecked")
        List<String> triggers = (List<String>) doc.get("triggers");
        return requiredKey == null
                || (triggers != null && triggers.contains(requiredKey));
    }

    private boolean passesSymptomFilter(QueryDocumentSnapshot doc) {
        int pos = spinnerSymptom.getSelectedItemPosition();
        if (pos == 0) return true; // "Any symptom"

        // We interpret "some/a lot" as value != "none"
        switch (pos) {
            case 1: // Night waking (some/a lot)
                return isProblem(doc.getString("nightWaking"));
            case 2: // Activity limits (some/a lot)
                return isProblem(doc.getString("activityLimits"));
            case 3: // Cough/wheeze (some/a lot)
                return isProblem(doc.getString("coughWheeze"));
            default:
                return true;
        }
    }

    private boolean isProblem(@Nullable String value) {
        return value != null && !"none".equals(value);
    }

    private void loadChildrenForFilter() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference childrenRef = db.collection("users")
                .document(uid)
                .collection("children");

        childrenRef.get().addOnSuccessListener(snapshot -> {
            childIds.clear();
            childNames.clear();

            // First option: All children
            childIds.add(null);
            childNames.add("All children");

            for (QueryDocumentSnapshot childDoc : snapshot) {
                String name = childDoc.getString("name");
                if (name == null || name.trim().isEmpty()) {
                    name = "Unnamed child";
                }
                childIds.add(childDoc.getId());
                childNames.add(name);
            }

            ArrayAdapter<String> childAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    childNames
            );
            childAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerChild.setAdapter(childAdapter);
        });
    }

    private void loadZoneHistory(String uid,
                                 @Nullable String filterChildId,
                                 String start,
                                 String end,
                                 boolean hasDateRange) {

        CollectionReference zoneRef = db.collection("users")
                .document(uid)
                .collection("zoneHistory");

        zoneRef.get().addOnSuccessListener(snapshot -> {

            for (QueryDocumentSnapshot doc : snapshot) {
                String date = doc.getString("date");
                if (hasDateRange && date != null) {
                    // Same date filter logic as daily check-ins
                    if (date.compareTo(start) < 0 || date.compareTo(end) > 0) {
                        continue;
                    }
                }

                String docChildId = doc.getString("childId");
                if (filterChildId != null && docChildId != null && !filterChildId.equals(docChildId)) {
                    continue;
                }

                String childName = doc.getString("childName");
                if (childName == null) childName = "Child";

                String oldZone = doc.getString("oldZone");
                String newZone = doc.getString("newZone");
                Long dailyPEF = doc.getLong("dailyPEF");
                Long pb = doc.getLong("pb");

                String nightLine = "Zone changed from " +
                        (oldZone != null ? oldZone : "?") +
                        " to " +
                        (newZone != null ? newZone : "?");

                String triggersLine = "PEF " +
                        (dailyPEF != null ? dailyPEF : 0) +
                        ", PB " +
                        (pb != null ? pb : 0);

                // Reuse HistoryEntry to display this
                HistoryEntry entry = new HistoryEntry(
                        childName,
                        date != null ? date : "",
                        nightLine,   // goes in the "Night:" row
                        "-",         // Activity
                        "-",         // Cough/wheeze
                        triggersLine,
                        "zone change"
                );

                historyItems.add(entry);
            }

            // Append to whatever is already shown
            historyAdapter.notifyDataSetChanged();
        });
    }
}
