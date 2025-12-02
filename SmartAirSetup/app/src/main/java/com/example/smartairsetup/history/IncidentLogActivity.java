package com.example.smartairsetup.history;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IncidentLogActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Spinner spinnerChildFilter;
    private Button buttonGetLogs;
    private Button buttonBack;
    private TextView textSymptomSummary;
    private ListView listHistory;

    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();
    private ArrayAdapter<String> childAdapter;

    private final List<String> incidentItems = new ArrayList<>();
    private ArrayAdapter<String> historyAdapter;

    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_log);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        spinnerChildFilter = findViewById(R.id.spinnerChildFilter);
        buttonGetLogs = findViewById(R.id.buttonGetLogs);
        buttonBack = findViewById(R.id.buttonBack);
        textSymptomSummary = findViewById(R.id.textSymptomSummary);
        listHistory = findViewById(R.id.listHistory);

        setupChildSpinner();
        setupHistoryList();

        buttonGetLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadIncidentLogsForSelectedChild();
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupChildSpinner() {
        childAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                childNames
        );
        childAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChildFilter.setAdapter(childAdapter);

        loadChildrenForCurrentParent();
    }
    private void setupHistoryList() {
        historyAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_incident_log,
                R.id.textIncidentBody,
                incidentItems
        );
        listHistory.setAdapter(historyAdapter);
    }


    private void loadChildrenForCurrentParent() {
        String parentUid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    childNames.clear();
                    childIds.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String childName = doc.getString("name");
                        String childId = doc.getId();

                        childNames.add(childName);
                        childIds.add(childId);
                    }

                    if (childNames.isEmpty()) {
                        Toast.makeText(
                                IncidentLogActivity.this,
                                "No children found",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    childAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        IncidentLogActivity.this,
                        "Failed to load children: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void loadIncidentLogsForSelectedChild() {
        int selected = spinnerChildFilter.getSelectedItemPosition();
        if (selected < 0 || selected >= childIds.size()) {
            Toast.makeText(this, "Please select a child.", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentUid = mAuth.getCurrentUser().getUid();
        String childId = childIds.get(selected);

        CollectionReference entriesRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("triage")
                .document("logs")
                .collection("entries");

        entriesRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    incidentItems.clear();

                    int totalCount = 0;

                    for (QueryDocumentSnapshot doc:querySnapshot) {
                        Long timestamp= doc.getLong("timestamp");
                        String zone =doc.getString("zone");
                        Boolean blueLipsNails = doc.getBoolean("blueLipsNails");
                        Boolean cantSpeakFullSentences = doc.getBoolean("cantSpeakFullSentences");
                        Boolean chestRetractions = doc.getBoolean("chestRetractions");
                        String userResponse =doc.getString("message-triage");
                        long optionalPEF = 0;
                        if(doc.contains("dailyPEF")){
                            optionalPEF = doc.getLong("dailyPEF");

                        }

                        if (timestamp ==null) {
                            continue;
                        }

                        totalCount++;

                        Date date= new Date(timestamp);
                        String dateStr = dateTimeFormat.format(date);

                        StringBuilder row =new StringBuilder();
                        row.append(dateStr);
                        row.append("  –  Zone: ");
                        if (zone != null) {
                            row.append(zone);
                        } else {
                            row.append("None");
                        }

                        List<String> flags = new ArrayList<>();
                        if (blueLipsNails != null && blueLipsNails) {
                            flags.add("Blue lips/nails");
                        }
                        if (cantSpeakFullSentences != null && cantSpeakFullSentences) {
                            flags.add("Cannot speak full sentences");
                        }
                        if (chestRetractions != null && chestRetractions) {
                            flags.add("Chest retractions");
                        }

                        if (!flags.isEmpty()) {
                            row.append("\nFlags: ");
                            for (int i = 0; i < flags.size(); i++) {
                                String flag = flags.get(i);
                                row.append(flag);
                                if (i < flags.size() - 1) {
                                    row.append(", ");
                                }
                            }
                        }

                        if (userResponse != null && !userResponse.trim().isEmpty()) {
                            row.append("\nUser response: ");
                            row.append(userResponse);
                        }

                        if(doc.contains("dailyPEF")){
                            row.append("\n Optional PEF: ");
                            row.append(optionalPEF);
                        }
                        //Hard Coded Guidance
                        row.append("\n");
                        row.append("\nGuidance:");
                        row.append("\nCall emergency services immediately");
                        row.append("\nUse rescue inhaler as prescribed while waiting");
                        row.append("\nKeep your child calm and upright");
                        row.append("\nDo not wait to see if symptoms improve");

                        incidentItems.add(row.toString());
                    }

                    historyAdapter.notifyDataSetChanged();

                })
                .addOnFailureListener(e -> Toast.makeText(
                        IncidentLogActivity.this,
                        "Failed to load incident logs: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
