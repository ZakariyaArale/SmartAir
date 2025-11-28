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

public class ZoneActivity extends AppCompatActivity {

    public Button chooseChildButton;
    public TextView zoneLabel;
    private FirebaseFirestore db;
    private String parentID;

    // NEW: track current state
    private String selectedChildUid = null;
    private String currentZone = null; // "GREEN"/"YELLOW"/"RED"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone);

        chooseChildButton = findViewById(R.id.chooseChildButton);
        zoneLabel = findViewById(R.id.zoneLabel);
        GradientDrawable background = (GradientDrawable) zoneLabel.getBackground();

        db = FirebaseFirestore.getInstance();
        parentID = "VfB95gwXXyWFAqdajTHJBgyeYfB3"; // hardcoded parent for testing

        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog childDiaglog = new ChildDiaglog(this, provider);

        chooseChildButton.setOnClickListener(v -> {
            childDiaglog.showSelectionDialog(chooseChildButton);
            // We don't know if dialog sets tag immediately, so we'll refresh in onResume()
        });

        // NEW: click zone label to open decision card for current zone
        zoneLabel.setOnClickListener(v -> {
            if (selectedChildUid == null) {
                Toast.makeText(this, "Select a child first.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentZone == null) {
                Toast.makeText(this, "Zone not available yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            openDecisionCard(false, parentID, selectedChildUid, currentZone);
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
        // Best-effort: if dialog set the tag, pick it up here and refresh
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
        // NEW: store selected child
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

                    String zone = latestDoc.getString("zone");
                    if (zone == null) {
                        currentZone = null;
                        background.setColor(Color.parseColor("#808080")); // grey
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
                            background.setColor(Color.parseColor("#808080")); // grey
                            break;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ZoneActivity", "Error fetching latest zone", e);
                    currentZone = null;
                    background.setColor(Color.parseColor("#808080")); // grey on error
                });
    }

    private void openDecisionCard(boolean isChild, String parentId, String childUid, String zone) {
        Class<?> target;
        switch (zone) {
            case "GREEN": target = GreenCardActivity.class; break;
            case "YELLOW": target = YellowCardActivity.class; break;
            case "RED": target = RedCardActivity.class; break;
            default:
                Toast.makeText(this, "Unknown zone.", Toast.LENGTH_SHORT).show();
                return;
        }

        Intent i = new Intent(this, target);
        i.putExtra(YellowCardActivity.EXTRA_IS_CHILD, isChild);
        i.putExtra(YellowCardActivity.EXTRA_CHILD_UID, childUid);
        i.putExtra("extra_parent_id", parentId); // optional, useful for Firestore paths
        startActivity(i);
    }
}