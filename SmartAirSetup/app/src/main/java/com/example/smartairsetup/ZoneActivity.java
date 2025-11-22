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
        parentID = "qGVzsSb3PMaI3D0UumcwJpuMgMG2";

        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog childDiaglog = new ChildDiaglog(this, provider);

        // If a child is already selected, update color
        Object tag = chooseChildButton.getTag();
        if (tag != null) {
            updateZoneColor(tag.toString(), background);
        }

        chooseChildButton.setOnClickListener(v -> childDiaglog.showSelectionDialog(chooseChildButton));
    }

    // Called when a child is selected or already set
    public void updateZoneColor(String childUid, GradientDrawable background) {
        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .collection("PEF")
                .document("latest")
                .get()
                .addOnSuccessListener(latestDoc -> {

                    Log.d("ZONE", "Firestore callback triggered");

                    if (!latestDoc.exists()) {
                        Log.d("ZONE", "No latest doc -> setting GREY");
                        background.setColor(Color.parseColor("#808080"));
                        return;
                    }

                    Long dailyPEF = latestDoc.getLong("dailyPEF");
                    Long pb = latestDoc.getLong("pb");

                    Log.d("ZONE", "dailyPEF=" + dailyPEF + ", pb=" + pb);
                    if (dailyPEF == null || pb == null || dailyPEF <= 0 || pb <= 0) {
                        Log.d("ZONE", "Invalid values -> setting GREY");
                        background.setColor(Color.parseColor("#808080"));
                        return;
                    }

                    double percentage = (double) dailyPEF / pb;
                    Log.d("ZONE", "percentage=" + percentage);

                    if (percentage >= 0.8) {
                        Log.d("ZONE", "Setting GREEN");
                        background.setColor(Color.parseColor("#4CAF50"));
                    } else if (percentage >= 0.5) {
                        Log.d("ZONE", "Setting YELLOW");
                        background.setColor(Color.parseColor("#FFC107"));
                    } else {
                        Log.d("ZONE", "Setting RED");
                        background.setColor(Color.parseColor("#F44336"));
                    }
                });
    }
}