package com.example.smartairsetup;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ZoneActivity extends AppCompatActivity {

    public Button chooseChildButton;
    public TextView zoneLabel;
    private FirebaseFirestore db;
    private String parentID;

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
        });

        // If a child is already selected, update the zone color
        Object tag = chooseChildButton.getTag();
        if (tag != null) {
            updateZoneColor(tag.toString(), background);
        }
    }

    public void updateZoneColor(String childUid, GradientDrawable background) {
        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .collection("PEF")
                .document("latest")
                .get()
                .addOnSuccessListener(latestDoc -> {
                    if (!latestDoc.exists()) {
                        background.setColor(Color.parseColor("#808080")); // grey
                        return;
                    }

                    String zone = latestDoc.getString("zone");

                    if (zone == null) {
                        background.setColor(Color.parseColor("#808080")); // grey
                        return;
                    }

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
                            background.setColor(Color.parseColor("#808080")); // grey
                            break;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ZoneActivity", "Error fetching latest zone", e);
                    background.setColor(Color.parseColor("#808080")); // grey on error
                });
    }
}