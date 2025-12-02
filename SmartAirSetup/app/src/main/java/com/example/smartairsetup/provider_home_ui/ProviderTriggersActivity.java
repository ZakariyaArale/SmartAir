package com.example.smartairsetup.provider_home_ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ProviderTriggersActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private TextView summary;
    private TextView empty;
    private ListView list;

    private final ArrayList<String> rows = new ArrayList<>();
    private android.widget.ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_triggers);

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
        summary = findViewById(R.id.textSummary);
        empty = findViewById(R.id.textEmpty);
        list = findViewById(R.id.listTriggers);

        title.setText((childName != null ? childName : "Child") + " • Triggers");

        adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(adapter);

        Button back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());

        listenForTriggers(parentUid, childId);
    }

    private void listenForTriggers(String parentUid, String childId) {
        // Check-ins live at: users/{parentUid}/dailyCheckins
        listener = db.collection("users")
                .document(parentUid)
                .collection("dailyCheckins")
                .whereEqualTo("childId", childId)
                .orderBy("date", Query.Direction.DESCENDING) // date is yyyy-MM-dd (string)
                .limit(180)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Toast.makeText(this, "Failed to load triggers: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    rows.clear();
                    Map<String, Integer> freq = new HashMap<>();

                    for (var doc : snap.getDocuments()) {
                        String date = doc.getString("date");
                        List<String> triggers = (List<String>) doc.get("triggers");

                        if (triggers == null) triggers = new ArrayList<>();

                        // frequency count
                        for (String t : triggers) {
                            if (t == null) continue;
                            String key = t.trim();
                            if (key.isEmpty()) continue;
                            freq.put(key, freq.getOrDefault(key, 0) + 1);
                        }

                        String prettyTriggers = triggers.isEmpty()
                                ? "none"
                                : TextUtils.join(", ", prettyList(triggers));

                        String line = (date != null ? date : "Unknown date") + "  •  " + prettyTriggers;
                        rows.add(line);
                    }

                    adapter.notifyDataSetChanged();

                    boolean isEmpty = rows.isEmpty();
                    empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

                    summary.setText(buildSummary(freq));
                });
    }

    private List<String> prettyList(List<String> raw) {
        List<String> out = new ArrayList<>();
        for (String t : raw) {
            if (t == null) continue;
            switch (t) {
                case "cold_air": out.add("cold air"); break;
                case "dust_pets": out.add("dust / pets"); break;
                case "strong_odors": out.add("strong odors"); break;
                default: out.add(t.replace('_', ' '));
            }
        }
        return out;
    }

    private String buildSummary(Map<String, Integer> freq) {
        if (freq.isEmpty()) {
            return "Most common triggers: none recorded";
        }

        // naive top-3 without extra deps
        String top1 = null, top2 = null, top3 = null;
        int c1 = 0, c2 = 0, c3 = 0;

        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            String k = e.getKey();
            int v = e.getValue();
            if (v > c1) { top3 = top2; c3 = c2; top2 = top1; c2 = c1; top1 = k; c1 = v; }
            else if (v > c2) { top3 = top2; c3 = c2; top2 = k; c2 = v; }
            else if (v > c3) { top3 = k; c3 = v; }
        }

        return "Most common triggers: "
                + prettyLabel(top1) + " (" + c1 + ")"
                + (top2 != null ? ", " + prettyLabel(top2) + " (" + c2 + ")" : "")
                + (top3 != null ? ", " + prettyLabel(top3) + " (" + c3 + ")" : "");
    }

    private String prettyLabel(String t) {
        if (t == null) return "-";
        switch (t) {
            case "cold_air": return "cold air";
            case "dust_pets": return "dust / pets";
            case "strong_odors": return "strong odors";
            default: return t.replace('_', ' ');
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}