package com.example.smartairsetup;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class ZoneActivity extends AppCompatActivity {

    public Button chooseChildButton;
    public Button backButton;
    public TextView zoneLabel;
    private FirebaseFirestore db;
    private String parentID; // parent UID received from intent

    // Track current state
    private String selectedChildUid = null;
    private String currentZone = null; // "GREEN"/"YELLOW"/"RED"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone);

        chooseChildButton = findViewById(R.id.chooseChildButton);
        backButton = findViewById(R.id.backButton);
        zoneLabel = findViewById(R.id.zoneLabel);
        GradientDrawable background = (GradientDrawable) zoneLabel.getBackground();
        background.setColor(Color.parseColor("#808080"));
        db = FirebaseFirestore.getInstance();

        // Get parent UID from intent
        if (getIntent() != null && getIntent().hasExtra("PARENT_UID")) {
            parentID = getIntent().getStringExtra("PARENT_UID");
        } else {
            Toast.makeText(this, "Parent UID not available.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog childDiaglog = new ChildDiaglog(this, provider);

        // Choose child button
        chooseChildButton.setOnClickListener(v -> childDiaglog.showSelectionDialog(chooseChildButton));

        // Back button just finishes the activity
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ParentHomeActivity.class);
            startActivity(intent);
        });

        // Zone label click shows current zone info
        zoneLabel.setOnClickListener(v -> {
            if (selectedChildUid == null) {
                Toast.makeText(this, "Select a child first.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentZone == null) {
                Toast.makeText(this, "Zone not available yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Current zone: " + currentZone, Toast.LENGTH_SHORT).show();
        });

        // If a child is already selected, update immediately
        Object tag = chooseChildButton.getTag();
        if (tag != null) {
            selectedChildUid = tag.toString();
            updateZoneColor(selectedChildUid, background);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Object tag = chooseChildButton.getTag();
        if (tag != null) {
            String newUid = tag.toString();
            if (!newUid.equals(selectedChildUid)) {
                selectedChildUid = newUid;
                GradientDrawable background = (GradientDrawable) zoneLabel.getBackground();
                updateZoneColor(selectedChildUid, background);
            }
        }
    }

    public void updateZoneColor(String childUid, GradientDrawable background) {
        selectedChildUid = childUid;

        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .collection("PEF")
                .document("latest")
                .get()
                .addOnSuccessListener(latestDoc -> {
                    if (!latestDoc.exists()) {
                        currentZone = null;
                        background.setColor(Color.parseColor("#808080")); // grey
                        return;
                    }

                    Long ts = latestDoc.getLong("timestamp");
                    if (ts == null) {
                        currentZone = null;
                        background.setColor(Color.parseColor("#808080"));
                        return;
                    }

                    Calendar recordCal = Calendar.getInstance();
                    recordCal.setTimeInMillis(ts);

                    Calendar todayCal = Calendar.getInstance();

                    boolean sameDay =
                            recordCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                    recordCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR);

                    if (!sameDay) {
                        currentZone = null;
                        background.setColor(Color.parseColor("#808080"));
                        return;
                    }

                    String zone = latestDoc.getString("zone");
                    if (zone == null) {
                        currentZone = null;
                        background.setColor(Color.parseColor("#808080"));
                        return;
                    }

                    currentZone = zone.toUpperCase();

                    switch (currentZone) {
                        case "GREEN":
                            background.setColor(Color.parseColor("#4CAF50"));
                            break;
                        case "YELLOW":
                            background.setColor(Color.parseColor("#FFC107"));
                            break;
                        case "RED":
                            background.setColor(Color.parseColor("#F44336"));
                            break;
                        default:
                            currentZone = null;
                            background.setColor(Color.parseColor("#808080"));
                            break;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ZoneActivity", "Error fetching latest zone", e);
                    currentZone = null;
                    background.setColor(Color.parseColor("#808080"));
                });
    }
}
