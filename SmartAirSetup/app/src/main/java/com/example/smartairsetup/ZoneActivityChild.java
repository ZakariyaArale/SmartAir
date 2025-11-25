package com.example.smartairsetup;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ZoneActivityChild extends AppCompatActivity {

    private Button chooseChildButton;
    private FirebaseFirestore db;

    // Forced child and parent UIDs for testing
    private final String childUid = "gifrbhr98mAAyv78MC80";
    private final String parentID = "VfB95gwXXyWFAqdajTHJBgyeYfB3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone_child);

        // UI references
        TextView zoneLabel = findViewById(R.id.zoneLabel);
        chooseChildButton = findViewById(R.id.chooseChildButton);
        GradientDrawable background = (GradientDrawable) zoneLabel.getBackground();

        db = FirebaseFirestore.getInstance();

        // Load child info immediately
        loadChildInfo(background);
    }

    private void loadChildInfo(GradientDrawable background) {
        // Fetch child document via parent
        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(childDoc -> {
                    if (!childDoc.exists()) {
                        chooseChildButton.setText("Unknown child");
                        background.setColor(Color.parseColor("#808080")); // Grey
                        return;
                    }

                    // Display child name
                    String childName = childDoc.getString("name");
                    chooseChildButton.setText(childName != null ? childName : "Invalid child");

                    // Fetch latest PEF document
                    db.collection("users")
                            .document(parentID)
                            .collection("children")
                            .document(childUid)
                            .collection("PEF")
                            .document("latest")
                            .get()
                            .addOnSuccessListener(latestDoc -> {
                                if (!latestDoc.exists()) {
                                    background.setColor(Color.parseColor("#808080")); // Grey
                                    return;
                                }

                                // Use stored zone if available
                                String zone = latestDoc.getString("zone");
                                if (zone != null) {
                                    setZoneColor(zone, background);
                                } else {
                                    // Compute zone if missing
                                    Long dailyPEF = latestDoc.getLong("dailyPEF");
                                    Long pb = latestDoc.getLong("pb");

                                    if (dailyPEF != null && pb != null && dailyPEF > 0 && pb > 0) {
                                        double percentage = (double) dailyPEF / pb;
                                        if (percentage >= 0.8) setZoneColor("GREEN", background);
                                        else if (percentage >= 0.5) setZoneColor("YELLOW", background);
                                        else setZoneColor("RED", background);
                                    } else {
                                        background.setColor(Color.parseColor("#808080")); // Grey
                                    }
                                }
                            });
                });
    }

    private void setZoneColor(String zone, GradientDrawable background) {
        switch (zone.toUpperCase()) {
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
                background.setColor(Color.parseColor("#808080")); // Grey
                break;
        }
    }
}