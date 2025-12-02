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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderRescueLogsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private RecyclerView recycler;
    private TextView emptyText;
    private TextView title;

    private ProviderRescueLogAdapter adapter;
    private final List<RescueLogItem> items = new ArrayList<>();

    // cache medId -> medName so we don’t re-fetch constantly
    private final Map<String, String> medNameCache = new HashMap<>();

    private String parentUid;
    private String childId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_rescue_logs);

        db = FirebaseFirestore.getInstance();

        parentUid = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_PARENT_UID);
        childId = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_CHILD_ID);
        String childName = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_CHILD_NAME);

        if (parentUid == null || childId == null) {
            Toast.makeText(this, "Missing child info", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        title = findViewById(R.id.textTitle);
        title.setText((childName != null ? childName : "Child") + " • Rescue Logs");

        emptyText = findViewById(R.id.textEmpty);
        recycler = findViewById(R.id.recyclerRescueLogs);

        adapter = new ProviderRescueLogAdapter(items);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        Button back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForRescueLogs();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    private void listenForRescueLogs() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }

        listener = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medLogs")
                .whereEqualTo("isRescue", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Toast.makeText(this, "Failed to load logs: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    items.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        items.add(RescueLogItem.from(doc));
                    }

                    adapter.notifyDataSetChanged();
                    hydrateMedicationNames(); // fill names after list arrives

                    boolean isEmpty = items.isEmpty();
                    emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                });
    }

    private void hydrateMedicationNames() {
        for (RescueLogItem item : items) {
            if (item.medId == null || item.medId.trim().isEmpty()) continue;

            // already cached
            if (medNameCache.containsKey(item.medId)) {
                item.medName = medNameCache.get(item.medId);
                continue;
            }

            db.collection("users")
                    .document(parentUid)
                    .collection("children")
                    .document(childId)
                    .collection("medications")
                    .document(item.medId)
                    .get()
                    .addOnSuccessListener(medSnap -> {
                        String name = medSnap.getString("name");
                        if (name == null || name.trim().isEmpty()) name = "Rescue medication";

                        medNameCache.put(item.medId, name);

                        // update all rows referencing same medId
                        for (RescueLogItem it : items) {
                            if (item.medId.equals(it.medId)) {
                                it.medName = name;
                            }
                        }
                        adapter.notifyDataSetChanged();
                    });
        }
    }
}