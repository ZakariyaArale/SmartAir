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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProviderControllerSummaryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private TextView textLastTaken;
    private TextView textCount7;
    private TextView textCount30;
    private TextView emptyText;

    private RecyclerView recycler;
    private ProviderControllerLogAdapter adapter;
    private final List<ControllerLogItem> items = new ArrayList<>();

    // medication lookup (medId -> name, and medId -> isRescue)
    private final Map<String, String> medIdToName = new HashMap<>();
    private final Map<String, Boolean> medIdToIsRescue = new HashMap<>();

    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_controller_summary);

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
        textLastTaken = findViewById(R.id.textLastTaken);
        textCount7 = findViewById(R.id.textCount7);
        textCount30 = findViewById(R.id.textCount30);
        emptyText = findViewById(R.id.textEmpty);
        recycler = findViewById(R.id.recyclerControllerLogs);

        title.setText((childName != null ? childName : "Child") + " • Controller Summary");

        adapter = new ProviderControllerLogAdapter(items);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        Button back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());

        // 1) load meds map first (so we can resolve names)
        loadMedicationsMap(parentUid, childId, () -> {
            // 2) then listen to controller logs
            listenForControllerLogs(parentUid, childId);
        });
    }

    private void loadMedicationsMap(String parentUid, String childId, Runnable onDone) {
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medications")
                .get()
                .addOnSuccessListener(qs -> {
                    medIdToName.clear();
                    medIdToIsRescue.clear();

                    qs.getDocuments().forEach(doc -> {
                        String medId = doc.getId();
                        String name = doc.getString("name");
                        Boolean isRescue = doc.getBoolean("isRescue");

                        if (name != null && !name.trim().isEmpty()) {
                            medIdToName.put(medId, name.trim());
                        }
                        if (isRescue != null) {
                            medIdToIsRescue.put(medId, isRescue);
                        }
                    });

                    onDone.run();
                })
                .addOnFailureListener(e -> {
                    // Not fatal; we can still show logs (med name may be generic)
                    onDone.run();
                });
    }

    private void listenForControllerLogs(String parentUid, String childId) {
        // If you ever see “requires an index”, click the console link and create it.
        listener = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medLogs")
                .whereEqualTo("isRescue", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Toast.makeText(this, "Failed to load controller logs: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    items.clear();

                    long now = System.currentTimeMillis();
                    long sevenAgo = now - (7L * 24 * 60 * 60 * 1000);
                    long thirtyAgo = now - (30L * 24 * 60 * 60 * 1000);

                    int count7 = 0;
                    int count30 = 0;

                    Date lastTaken = null;

                    // Protect against duplicate docs (rare, but can happen if you accidentally add twice)
                    Set<String> seenIds = new HashSet<>();

                    for (var doc : snap.getDocuments()) {
                        if (!seenIds.add(doc.getId())) continue;

                        ControllerLogItem item = ControllerLogItem.from(doc, medIdToName, medIdToIsRescue);
                        if (item == null) continue;

                        items.add(item);

                        if (item.takenAt != null) {
                            long t = item.takenAt.getTime();
                            if (lastTaken == null || t > lastTaken.getTime()) lastTaken = item.takenAt;
                            if (t >= sevenAgo) count7++;
                            if (t >= thirtyAgo) count30++;
                        }
                    }

                    adapter.notifyDataSetChanged();

                    boolean isEmpty = items.isEmpty();
                    emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

                    textLastTaken.setText("Last controller dose: " + (lastTaken != null ? fmt.format(lastTaken) : "-"));
                    textCount7.setText("Doses (last 7 days): " + count7);
                    textCount30.setText("Doses (last 30 days): " + count30);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}