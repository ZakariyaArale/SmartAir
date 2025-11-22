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

    // Forced child selection
    private final String childUid = "sUjssxQr1fYWZ8UaevXU5FV0vY03";
    private final String parentID = "qGVzsSb3PMaI3D0UumcwJpuMgMG2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone_child);

        // UI refs
        TextView zoneLabel = findViewById(R.id.zoneLabel);
        chooseChildButton = findViewById(R.id.chooseChildButton);

        // Background of zoneLabel
        GradientDrawable background = (GradientDrawable) zoneLabel.getBackground();

        db = FirebaseFirestore.getInstance();

        // Load child info immediately when screen loads
        loadChildInfo(background);
    }

    private void loadChildInfo(GradientDrawable background) {
        // First fetch the child document for the name
        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(childDoc -> {
                    if (childDoc.exists()) {
                        String childName = childDoc.getString("name");
                        if (childName != null) {
                            chooseChildButton.setText(childName);
                        }
                        else{
                            chooseChildButton.setText("Invalid child");
                            background.setColor(Color.parseColor("#808080")); // Grey
                            return; // Stop processing
                        }
                    }

                    // Now fetch latest PEF for dailyPEF / pb
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

                                Long dailyPEF = latestDoc.getLong("dailyPEF");
                                Long pb = latestDoc.getLong("pb");


                                if (dailyPEF == null || dailyPEF <= 0 || pb == null || pb <= 0) {
                                    background.setColor(Color.parseColor("#808080")); // Grey
                                    return;
                                }

                                double percentage = (double) dailyPEF / pb;

                                if (percentage >= 0.8) {
                                    background.setColor(Color.parseColor("#4CAF50")); // Green
                                } else if (percentage >= 0.5) {
                                    background.setColor(Color.parseColor("#FFC107")); // Yellow
                                } else {
                                    background.setColor(Color.parseColor("#F44336")); // Red
                                }
                            });
                });
    }

}