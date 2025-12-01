package com.example.smartairsetup.parent_home_ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.example.smartairsetup.triage.EmergencyActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ParentFamilyActivity extends AbstractNavigation {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout familyListContainer;
    private Button addMemberButton;

    // Tracks the currently visible confirmation panel
    private View currentConfirmationView;

    // Tracks the currently selected child row
    private View currentSelectedRow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        familyListContainer = findViewById(R.id.familyListContainer);
        addMemberButton = findViewById(R.id.addMemberButton);

        loadChildrenFromFirestore();

        addMemberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ParentFamilyActivity.this, AddChildActivity.class);
                startActivity(intent);
            }
        });
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

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmationForChild(childId, name, rowView);
            }
        });

        familyListContainer.addView(rowView);
    }

    private void showConfirmationForChild(String childId, String name, View rowView) {
        // Reset background of previously selected row
        if (currentSelectedRow != null && currentSelectedRow != rowView) {
            currentSelectedRow.setBackgroundColor(Color.TRANSPARENT);
        }

        // Set background of newly selected row to gray
        rowView.setBackgroundColor(Color.parseColor("#DDDDDD"));
        currentSelectedRow = rowView;

        // Remove existing confirmation panel if any
        if (currentConfirmationView != null) {
            ViewGroup parent = (ViewGroup) currentConfirmationView.getParent();
            if (parent != null) {
                parent.removeView(currentConfirmationView);
            }
            currentConfirmationView = null;
        }

        // Create a small confirmation panel under the selected row
        LinearLayout confirmLayout = new LinearLayout(this);
        confirmLayout.setOrientation(LinearLayout.VERTICAL);
        confirmLayout.setBackgroundColor(Color.parseColor("#EEF5FF"));
        int paddingHorizontal = dpToPx(16);
        int paddingVerticalTop = dpToPx(8);
        int paddingVerticalBottom = dpToPx(16);
        confirmLayout.setPadding(
                paddingHorizontal,
                paddingVerticalTop,
                paddingHorizontal,
                paddingVerticalBottom
        );

        TextView message = new TextView(this);
        message.setText("You will be redirected to " + name + "'s page.");
        message.setTextSize(14);

        Button goButton = new Button(this);
        goButton.setText("Go to child page");

        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.bottomMargin = dpToPx(8);
        message.setLayoutParams(messageParams);

        confirmLayout.addView(message);
        confirmLayout.addView(goButton);

        int rowIndex = familyListContainer.indexOfChild(rowView);
        if (rowIndex == -1) {
            familyListContainer.addView(confirmLayout);
        } else {
            familyListContainer.addView(confirmLayout, rowIndex + 1);
        }

        currentConfirmationView = confirmLayout;

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ParentFamilyActivity.this, ChildHomeActivity.class);
                intent.putExtra("CHILD_ID", childId);
                intent.putExtra("CHILD_NAME", name);
                startActivity(intent);
            }
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        float value = dp * density + 0.5f;
        return (int) value;
    }

    @Override
    protected void onHomeClicked() {
        Intent intent = new Intent(this, ParentHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onFamilyClicked() {
        // Already on Family screen
    }

    @Override
    protected void onEmergencyClicked() {
        Intent intent = new Intent(this, EmergencyActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        Intent intent = new Intent(this, ParentSettingsActivity.class);
        startActivity(intent);
    }
}
