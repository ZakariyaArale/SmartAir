package com.example.smartairsetup.zone;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.child_home_ui.ChildHomeActivity;
import com.example.smartairsetup.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class ZoneActivityChild extends AppCompatActivity {

    private Button chooseChildButton;
    public Button backButton;
    private FirebaseFirestore db;

    // Receive these from intent
    private String childUid;
    private String parentID;

    private String currentZone = null;
    private TextView zoneLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone_child);

        zoneLabel = findViewById(R.id.zoneLabel);
        chooseChildButton = findViewById(R.id.chooseChildButton);
        backButton = findViewById(R.id.backButton);
        GradientDrawable background = (GradientDrawable) zoneLabel.getBackground();

        db = FirebaseFirestore.getInstance();

        // Get childId and parentId from intent
        childUid = getIntent().getStringExtra("CHILD_ID");
        parentID = getIntent().getStringExtra("PARENT_UID");

        if (childUid == null || parentID == null) {
            Toast.makeText(this, "Missing child or parent ID.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadChildInfo(background);

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChildHomeActivity.class);
            if (childUid != null && !childUid.isEmpty()) {
                intent.putExtra("CHILD_ID", childUid);
                intent.putExtra("PARENT_UID", parentID);
            }
            startActivity(intent);
        });
    }

    private void loadChildInfo(GradientDrawable background) {
        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(childDoc -> {

                    if (!childDoc.exists()) {
                        chooseChildButton.setText("Unknown child");
                        currentZone = null;
                        background.setColor(Color.parseColor("#808080"));
                        return;
                    }

                    String childName = childDoc.getString("name");
                    chooseChildButton.setText(childName != null ? childName : "Invalid child");

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
                                    background.setColor(Color.parseColor("#808080"));
                                    return;
                                }

                                Long ts = latestDoc.getLong("timestamp");  // must be in millis
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

                                if (zone != null) {
                                    setZoneColor(zone, background);
                                } else {
                                    Long dailyPEF = latestDoc.getLong("dailyPEF");
                                    Long pb = latestDoc.getLong("pb");

                                    if (dailyPEF != null && pb != null && dailyPEF > 0 && pb > 0) {
                                        double percentage = (double) dailyPEF / pb;

                                        if (percentage >= 0.8) setZoneColor("GREEN", background);
                                        else if (percentage >= 0.5) setZoneColor("YELLOW", background);
                                        else setZoneColor("RED", background);
                                    } else {
                                        currentZone = null;
                                        background.setColor(Color.parseColor("#808080"));
                                    }
                                }
                            });
                });
    }

    private void setZoneColor(String zone, GradientDrawable background) {
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
    }
}
