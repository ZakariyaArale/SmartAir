package com.example.smartairsetup.parent_home_ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.smartairsetup.navigation.AbstractNavigation;
import com.example.smartairsetup.login.AddChildActivity;
import com.example.smartairsetup.R;
import com.example.smartairsetup.child_home_ui.ChildHomeActivity;
import com.example.smartairsetup.onboarding.OnboardingActivity;
import com.example.smartairsetup.triage.EmergencyActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ParentFamilyActivity extends AbstractNavigation {

    private static final String TAG = "ParentFamilyActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout familyListContainer;
    private Button addMemberButton;

    private View currentConfirmationView;
    private View currentSelectedRow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // AbstractNavigation sets the layout

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views AFTER setContentView
        familyListContainer = findViewById(R.id.familyListContainer);
        addMemberButton = findViewById(R.id.addMemberButton);

        if (addMemberButton == null) {
            Log.e(TAG, "addMemberButton is NULL! Check activity_parent_family.xml layout.");
        } else {
            Log.d(TAG, "addMemberButton found, setting click listener.");

            addMemberButton.setOnClickListener(view -> {
                Log.d(TAG, "Add Member button clicked!");
                Intent intent = new Intent(ParentFamilyActivity.this, AddChildActivity.class);
                startActivity(intent);
                Log.d(TAG, "Intent to AddChildActivity started.");
            });
        }

        loadChildrenFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildrenFromFirestore();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_parent_family;
    }

    private void loadChildrenFromFirestore() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentUid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    familyListContainer.removeAllViews();
                    currentConfirmationView = null;
                    currentSelectedRow = null;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String childId = doc.getId();
                        String name = doc.getString("name");
                        addFamilyRow(childId, name, "Child");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load children", e);
                    Toast.makeText(this, "Failed to load children.", Toast.LENGTH_SHORT).show();
                });
    }

    private void addFamilyRow(String childId, String name, String role) {
        View rowView = getLayoutInflater().inflate(
                R.layout.item_family_member,
                familyListContainer,
                false
        );

        ImageView avatarImage = rowView.findViewById(R.id.imageAvatar);
        TextView nameText = rowView.findViewById(R.id.textName);
        TextView roleText = rowView.findViewById(R.id.textRole);

        nameText.setText(name);
        roleText.setText(role);
        avatarImage.setImageResource(R.drawable.person);

        rowView.setOnClickListener(view -> showConfirmationForChild(childId, name, rowView));

        familyListContainer.addView(rowView);
    }

    private void showConfirmationForChild(String childId, String name, View rowView) {
        if (currentSelectedRow != null && currentSelectedRow != rowView) {
            currentSelectedRow.setBackgroundColor(Color.TRANSPARENT);
        }

        rowView.setBackgroundColor(Color.parseColor("#DDDDDD"));
        currentSelectedRow = rowView;

        if (currentConfirmationView != null) {
            ViewGroup parent = (ViewGroup) currentConfirmationView.getParent();
            if (parent != null) parent.removeView(currentConfirmationView);
            currentConfirmationView = null;
        }

        LinearLayout confirmLayout = new LinearLayout(this);
        confirmLayout.setOrientation(LinearLayout.VERTICAL);
        confirmLayout.setBackgroundColor(Color.parseColor("#EEF5FF"));
        int paddingHorizontal = dpToPx(16);
        int paddingVerticalTop = dpToPx(8);
        int paddingVerticalBottom = dpToPx(16);
        confirmLayout.setPadding(paddingHorizontal, paddingVerticalTop, paddingHorizontal, paddingVerticalBottom);

        TextView message = new TextView(this);
        message.setText("You will be redirected to " + name + "'s page.");
        message.setTextSize(14);

        Button goButton = new Button(this);
        goButton.setText("Go to child page");

        confirmLayout.addView(message);
        confirmLayout.addView(goButton);

        int rowIndex = familyListContainer.indexOfChild(rowView);
        if (rowIndex == -1) {
            familyListContainer.addView(confirmLayout);
        } else {
            familyListContainer.addView(confirmLayout, rowIndex + 1);
        }

        currentConfirmationView = confirmLayout;

        goButton.setOnClickListener(view -> {
            Log.d(TAG, "Go to child page clicked: " + name);

            String parentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
            if (parentUid == null) {
                Toast.makeText(this, "Parent UID not available.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check childAccounts for firstTime
            db.collection("childAccounts")
                    .whereEqualTo("childDocId", childId)
                    .whereEqualTo("parentUid", parentUid)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            boolean firstTimeFound = false;
                            for (DocumentSnapshot doc : querySnapshot) {
                                Boolean firstTime = doc.getBoolean("firstTime");
                                if (firstTime != null && firstTime) {
                                    firstTimeFound = true;

                                    // Update firstTime to false
                                    doc.getReference().update("firstTime", false)
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "firstTime set to false"))
                                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update firstTime", e));

                                    // Navigate to onboarding
                                    Intent onboardingIntent = new Intent(ParentFamilyActivity.this, OnboardingActivity.class);
                                    onboardingIntent.putExtra("PARENT_UID", parentUid);
                                    onboardingIntent.putExtra("CHILD_ID", childId);
                                    onboardingIntent.putExtra("firstTime", true);
                                    startActivity(onboardingIntent);
                                    finish();
                                    return;
                                }
                            }
                            // If no firstTime true, go to ChildHomeActivity
                            if (!firstTimeFound) {
                                Intent childIntent = new Intent(ParentFamilyActivity.this, ChildHomeActivity.class);
                                childIntent.putExtra("CHILD_ID", childId);
                                childIntent.putExtra("CHILD_NAME", name);
                                startActivity(childIntent);
                            }
                        } else {
                            Log.e(TAG, "No matching child found in childAccounts");
                            // Fallback to ChildHomeActivity
                            Intent childIntent = new Intent(ParentFamilyActivity.this, ChildHomeActivity.class);
                            childIntent.putExtra("CHILD_ID", childId);
                            childIntent.putExtra("CHILD_NAME", name);
                            startActivity(childIntent);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to query childAccounts", e);
                        Toast.makeText(this, "Error checking child account.", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    @Override
    protected void onHomeClicked() {
        startActivity(new Intent(this, ParentHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    @Override
    protected void onFamilyClicked() {
        // Already on Family screen
    }

    @Override
    protected void onEmergencyClicked() {
        startActivity(new Intent(this, EmergencyActivity.class));
    }

    @Override
    protected void onSettingsClicked() {
        startActivity(new Intent(this, ParentSettingsActivity.class));
    }
}
