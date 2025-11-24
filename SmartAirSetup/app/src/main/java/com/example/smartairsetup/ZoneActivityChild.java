package com.example.smartairsetup;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ZoneActivityChild extends AppCompatActivity {

    private Button chooseChildButton;
    private FirebaseFirestore db;

    // Get current logged-in child UID
    private final String childUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private String parentID;

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

        // Step 1: Fetch parent UID from childAccounts
        db.collection("childAccounts")
                .document(childUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        parentID = documentSnapshot.getString("parentUid");
                        Log.d("Firestore", "Parent UID: " + parentID);

                        // Step 2: Now that we have parentID, load child info
                        loadChildInfo(background);
                    } else {
                        Log.d("Firestore", "Child not found in childAccounts");
                        chooseChildButton.setText("Invalid child");
                        background.setColor(Color.parseColor("#808080")); // Grey
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching parent UID", e);
                    chooseChildButton.setText("Error");
                    background.setColor(Color.parseColor("#808080")); // Grey
                });
    }

    private void loadChildInfo(GradientDrawable background) {
        if (parentID == null) return;

        // Fetch child document for the name
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
                        } else {
                            chooseChildButton.setText("Invalid child");
                            background.setColor(Color.parseColor("#808080")); // Grey
                            return;
                        }
                    } else {
                        chooseChildButton.setText("Invalid child");
                        background.setColor(Color.parseColor("#808080")); // Grey
                        return;
                    }

                    // Fetch latest PEF
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
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error loading child info", e);
                    chooseChildButton.setText("Error");
                    background.setColor(Color.parseColor("#808080")); // Grey
                });
    }
}