package com.example.smartairsetup;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class ZoneActivityChild extends AppCompatActivity {

    private Button chooseChildButton;
    private FirebaseFirestore db;

    // Forced child and parent UIDs for testing
    private final String childUid = "gifrbhr98mAAyv78MC80";
    private final String parentID = "VfB95gwXXyWFAqdajTHJBgyeYfB3";

    // NEW
    private String currentZone = null;
    private TextView zoneLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone_child);

        zoneLabel = findViewById(R.id.zoneLabel);
        chooseChildButton = findViewById(R.id.chooseChildButton);
        GradientDrawable background = (GradientDrawable) zoneLabel.getBackground();

        db = FirebaseFirestore.getInstance();

        // NEW: click zone label to open decision card
        zoneLabel.setOnClickListener(v -> {
            if (currentZone == null) {
                Toast.makeText(this, "Zone not available yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            openDecisionCard(true, parentID, childUid, currentZone);
        });

        loadChildInfo(background);
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

                                // ✅ NEW: Check that this record is from TODAY
                                Long ts = latestDoc.getLong("timestamp");  // must be saved in millis
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
                                    // NOT today's data → turn grey
                                    currentZone = null;
                                    background.setColor(Color.parseColor("#808080"));
                                    return;
                                }
                                // ⬆️ END timestamp check

                                String zone = latestDoc.getString("zone");

                                if (zone != null) {
                                    setZoneColor(zone, background);
                                } else {
                                    // Fallback zone calculation
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
        i.putExtra("extra_parent_id", parentId);
        startActivity(i);
    }
}
