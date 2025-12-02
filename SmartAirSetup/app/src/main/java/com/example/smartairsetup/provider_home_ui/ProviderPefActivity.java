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

public class ProviderPefActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private TextView title;
    private TextView summary;
    private TextView emptyText;
    private RecyclerView recycler;

    private final List<PefLogItem> items = new ArrayList<>();
    private ProviderPefLogAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_pef);

        db = FirebaseFirestore.getInstance();

        String parentUid = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_PARENT_UID);
        String childId = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_CHILD_ID);
        String childName = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_CHILD_NAME);

        if (parentUid == null || childId == null) {
            Toast.makeText(this, "Missing child info", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        title = findViewById(R.id.textTitle);
        summary = findViewById(R.id.textSummary);
        emptyText = findViewById(R.id.textEmpty);
        recycler = findViewById(R.id.recyclerPefLogs);

        title.setText((childName != null ? childName : "Child") + " • PEF Logs");

        adapter = new ProviderPefLogAdapter(items);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        Button back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());

        listenForPefLogs(parentUid, childId);
    }

    private void listenForPefLogs(String parentUid, String childId) {
        // Path: users/{parentUid}/children/{childId}/PEF/logs/daily/{dateDoc}
        listener = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("PEF")
                .document("logs")
                .collection("daily")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(120)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Toast.makeText(this, "Failed to load PEF: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    items.clear();
                    for (var doc : snap.getDocuments()) {
                        items.add(PefLogItem.from(doc));
                    }
                    adapter.notifyDataSetChanged();

                    boolean isEmpty = items.isEmpty();
                    emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

                    if (isEmpty) {
                        summary.setText("No PEF logs shared for this child.");
                    } else {
                        // quick “latest” summary
                        PefLogItem latest = items.get(0);
                        summary.setText(
                                "Latest: " + safe(latest.date) +
                                        " • Daily PEF " + latest.dailyPEF +
                                        " • Zone " + safe(latest.zone) +
                                        " • PB " + latest.pb
                        );
                    }
                });
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}