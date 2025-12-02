package com.example.smartairsetup.provider_home_ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartairsetup.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProviderChildPortalActivity extends AppCompatActivity {

    public static final String EXTRA_PARENT_UID = "extra_parent_uid";
    public static final String EXTRA_CHILD_ID = "extra_child_id";
    public static final String EXTRA_CHILD_NAME = "extra_child_name";

    private FirebaseFirestore db;
    private DocumentReference childDocRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_child_portal);

        db = FirebaseFirestore.getInstance();

        String parentUid = getIntent().getStringExtra(EXTRA_PARENT_UID);
        String childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        String childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);

        if (parentUid == null || childId == null) {
            Toast.makeText(this, "Missing child info", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView title = findViewById(R.id.textProviderChildTitle);
        title.setText(childName != null ? childName : "Child");

        Button back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());

        Button btnRescue = findViewById(R.id.btnProviderRescueLogs);
        Button btnSymptoms = findViewById(R.id.btnProviderSymptoms);
        Button btnPEF = findViewById(R.id.btnProviderPEF);
        Button btnCharts = findViewById(R.id.btnProviderCharts);

        childDocRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId);

        childDocRef.addSnapshotListener((snap, err) -> {
            if (err != null || snap == null || !snap.exists()) return;

            boolean shareRescue = Boolean.TRUE.equals(snap.getBoolean("shareRescueLogs"));
            boolean shareSymptoms = Boolean.TRUE.equals(snap.getBoolean("shareSymptoms"));
            boolean sharePEF = Boolean.TRUE.equals(snap.getBoolean("sharePEF"));
            boolean shareCharts = Boolean.TRUE.equals(snap.getBoolean("shareSummaryCharts"));

            btnRescue.setVisibility(shareRescue ? View.VISIBLE : View.GONE);
            btnSymptoms.setVisibility(shareSymptoms ? View.VISIBLE : View.GONE);
            btnPEF.setVisibility(sharePEF ? View.VISIBLE : View.GONE);
            btnCharts.setVisibility(shareCharts ? View.VISIBLE : View.GONE);
        });

        btnRescue.setOnClickListener(v -> {
            Intent i = new Intent(this, ProviderRescueLogsActivity.class);
            i.putExtra(EXTRA_PARENT_UID, parentUid);
            i.putExtra(EXTRA_CHILD_ID, childId);
            i.putExtra(EXTRA_CHILD_NAME, childName);
            startActivity(i);
        });

        btnSymptoms.setOnClickListener(v -> {
            Intent i = new Intent(this, ProviderSymptomsActivity.class);
            i.putExtra(EXTRA_PARENT_UID, parentUid);
            i.putExtra(EXTRA_CHILD_ID, childId);
            i.putExtra(EXTRA_CHILD_NAME, childName);
            startActivity(i);
        });

        btnPEF.setOnClickListener(v -> {
            Intent i = new Intent(this, ProviderPefActivity.class);
            i.putExtra(EXTRA_PARENT_UID, parentUid);
            i.putExtra(EXTRA_CHILD_ID, childId);
            i.putExtra(EXTRA_CHILD_NAME, childName);
            startActivity(i);
        });

        btnCharts.setOnClickListener(v -> {
            Intent i = new Intent(this, ProviderChartsActivity.class);
            i.putExtra(EXTRA_PARENT_UID, parentUid);
            i.putExtra(EXTRA_CHILD_ID, childId);
            i.putExtra(EXTRA_CHILD_NAME, childName);
            startActivity(i);
        });
    }
}