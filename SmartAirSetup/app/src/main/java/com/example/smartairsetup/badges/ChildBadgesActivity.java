package com.example.smartairsetup.badges;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smartairsetup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ChildBadgesActivity extends AppCompatActivity {

    private ImageView imageBadgePerfectWeek;
    private ImageView imageBadgeTechnique;
    private ImageView imageBadgeLowRescueMonth;
    private TextView textBadgeTechniqueDesc;

    private FirebaseFirestore db;

    // Intent keys
    public static final String EXTRA_PARENT_ID = "EXTRA_PARENT_ID";
    public static final String EXTRA_CHILD_ID = "EXTRA_CHILD_ID";

    private String parentUid;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_badge);

        // Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        // UI
        TextView textBackBadges = findViewById(R.id.textBackBadges);
        imageBadgePerfectWeek = findViewById(R.id.imageBadgePerfectWeek);
        imageBadgeTechnique = findViewById(R.id.imageBadgeTechnique);
        imageBadgeLowRescueMonth = findViewById(R.id.imageBadgeLowRescueMonth);
        textBadgeTechniqueDesc = findViewById(R.id.textBadgeTechniqueDesc);

        textBackBadges.setOnClickListener(view -> finish());

        // Parent UID: by default, the FirebaseAuth current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            parentUid = currentUser.getUid();
        } else {
            parentUid = getIntent().getStringExtra(EXTRA_PARENT_ID);
        }

        // Child ID: first try EXTRA_CHILD_ID, then fallback to "CHILD_ID"
        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        if (childId == null || childId.isEmpty()) {
            childId = getIntent().getStringExtra("CHILD_ID");
        }

        if (parentUid == null || childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Missing parent or child id for badges.", Toast.LENGTH_SHORT).show();
            updateBadgesUI(false, false, false);
            return;
        }

        loadBadgeStatusAndUpdateUI();
    }

    private void loadBadgeStatusAndUpdateUI() {
        if (parentUid == null || childId == null) {
            updateBadgesUI(false, false, false);
            return;
        }

        // Default description in case Firestore does not have target field yet
        textBadgeTechniqueDesc.setText("Use perfect inhaler technique 10 times.");

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

                    Boolean earnedFlag = doc.getBoolean("earned");
                    Long targetLong = doc.getLong("target");
                    Long progressLong = doc.getLong("progress");

                    // Technique sessions badge: update description text dynamically
                    if ("technique_sessions".equals(badgeId)) {
                        if (targetLong != null) {
                            int target = targetLong.intValue();
                            String desc = "Use perfect inhaler technique " + target + " times.";
                            textBadgeTechniqueDesc.setText(desc);
                        }

                        boolean earned = false;
                        if (earnedFlag != null && earnedFlag) {
                            earned = true;
                        } else if (targetLong != null && progressLong != null && progressLong >= targetLong) {
                            earned = true;
                        }

                        if (earned) {
                            hasTechnique = true;
                        }

                        // continue so we do not process this doc again below
                        continue;
                    }

                    // Other badges (perfect week, low rescue month)
                    boolean earned = false;
                    if (earnedFlag != null && earnedFlag) {
                        earned = true;
                    } else if (targetLong != null && progressLong != null && progressLong >= targetLong) {
                        earned = true;
                    }

                    if (!earned) {
                        continue;
                    }

                    if ("perfect_controller_week".equals(badgeId)) {
                        hasPerfectWeek = true;
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

        int tintColorResId = R.color.button_icon_color;

        if (hasPerfectWeek) {
            imageBadgePerfectWeek.setImageTintList(
                    ContextCompat.getColorStateList(this, tintColorResId)
            );
        } else {
            imageBadgePerfectWeek.setImageTintList(null);
        }

        if (hasTechnique) {
            imageBadgeTechnique.setImageTintList(
                    ContextCompat.getColorStateList(this, tintColorResId)
            );
        } else {
            imageBadgeTechnique.setImageTintList(null);
        }

        if (hasLowRescueMonth) {
            imageBadgeLowRescueMonth.setImageTintList(
                    ContextCompat.getColorStateList(this, tintColorResId)
            );
        } else {
            imageBadgeLowRescueMonth.setImageTintList(null);
        }
    }
}
