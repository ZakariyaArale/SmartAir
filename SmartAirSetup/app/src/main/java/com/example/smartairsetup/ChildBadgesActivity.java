package com.example.smartairsetup;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ChildBadgesActivity extends AppCompatActivity {

    // UI
    private TextView textBackBadges;
    private ImageView imageBadgePerfectWeek;
    private ImageView imageBadgeTechnique10;
    private ImageView imageBadgeLowRescueMonth;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Parent and child IDs
    public static final String EXTRA_PARENT_ID = "EXTRA_PARENT_ID";
    public static final String EXTRA_CHILD_ID = "EXTRA_CHILD_ID";

    private String parentUid;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_badge);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // View binding
        textBackBadges = findViewById(R.id.textBackBadges);
        imageBadgePerfectWeek = findViewById(R.id.imageBadgePerfectWeek);
        imageBadgeTechnique10 = findViewById(R.id.imageBadgeTechnique10);
        imageBadgeLowRescueMonth = findViewById(R.id.imageBadgeLowRescueMonth);

        textBackBadges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        parentUid = getIntent().getStringExtra(EXTRA_PARENT_ID);
        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);

        loadBadgeStatusAndUpdateUI();
    }

    private void loadBadgeStatusAndUpdateUI() {
        if (parentUid == null || childId == null) {
            updateBadgesUI(false, false, false);
            return;
        }

        CollectionReference badgesRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("badges");

        badgesRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                updateBadgesUI(false, false, false);
                return;
            }

            boolean hasPerfectWeek = false;
            boolean hasTechnique = false;
            boolean hasLowRescueMonth = false;

            QuerySnapshot snapshot = task.getResult();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    String badgeId = doc.getId();
                    Long targetLong = doc.getLong("target");
                    Long progressLong = doc.getLong("progress");

                    if (targetLong == null || progressLong == null) {
                        continue;
                    }

                    boolean earned = progressLong >= targetLong;

                    if (!earned) {
                        continue;
                    }

                    if ("perfect_controller_week".equals(badgeId)) {
                        hasPerfectWeek = true;
                    } else if ("technique_sessions".equals(badgeId)) {
                        hasTechnique = true;
                    } else if ("low_rescue_month".equals(badgeId)) {
                        hasLowRescueMonth = true;
                    }
                }
            }

            updateBadgesUI(hasPerfectWeek, hasTechnique, hasLowRescueMonth);
        });
    }

    private void updateBadgesUI(boolean hasPerfectWeek,
                                boolean hasTechnique,
                                boolean hasLowRescueMonth) {

         int successColor = ContextCompat.getColor(this, R.color.button_icon_color);

        if (hasPerfectWeek) {
            imageBadgePerfectWeek.setImageTintList(
                    ContextCompat.getColorStateList(this, successColor)
            );
        }

        if (hasTechnique) {
            imageBadgeTechnique10.setImageTintList(
                    ContextCompat.getColorStateList(this, successColor)
            );
        }

        if (hasLowRescueMonth) {
            imageBadgeLowRescueMonth.setImageTintList(
                    ContextCompat.getColorStateList(this, successColor)
            );
        }
    }
}
