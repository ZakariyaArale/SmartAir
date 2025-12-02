package com.example.smartairsetup.provider_home_ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartairsetup.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ProviderTriageIncidentsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private TextView textSummary;
    private TextView emptyText;
    private RecyclerView recycler;

    private ProviderTriageIncidentAdapter adapter;
    private final List<TriageIncidentItem> items = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_triage_incidents);

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
        textSummary = findViewById(R.id.textSummary);
        emptyText = findViewById(R.id.textEmpty);
        recycler = findViewById(R.id.recyclerTriage);

        title.setText((childName != null ? childName : "Child") + " • Triage Incidents");

        adapter = new ProviderTriageIncidentAdapter(items);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        Button back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());

        listenForTriageIncidents(parentUid, childId);
    }

    private void listenForTriageIncidents(String parentUid, String childId) {
        // Path: users/{parentUid}/children/{childId}/PEF/logs/daily/{dateDoc}
        listener = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("PEF")
                .document("logs")
                .collection("daily")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(180) // ~6 months if you log daily
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Toast.makeText(this, "Failed to load triage incidents: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    items.clear();

                    int yellowCount = 0;
                    int redCount = 0;

                    for (var doc : snap.getDocuments()) {
                        TriageIncidentItem item = TriageIncidentItem.from(doc);
                        if (item == null) continue;

                        // Consider anything not GREEN an “incident”
                        if (item.zone == null) continue;
                        String z = item.zone.trim().toUpperCase();

                        if ("GREEN".equals(z)) continue;

                        if ("YELLOW".equals(z)) yellowCount++;
                        if ("RED".equals(z)) redCount++;

                        items.add(item);
                    }

                    adapter.notifyDataSetChanged();

                    boolean isEmpty = items.isEmpty();
                    emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

                    textSummary.setText(
                            "Incidents found: " + items.size() +
                                    "  •  Yellow: " + yellowCount +
                                    "  •  Red: " + redCount
                    );
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}