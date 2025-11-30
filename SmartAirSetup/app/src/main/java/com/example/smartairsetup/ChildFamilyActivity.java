package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChildFamilyActivity extends AbstractNavigation {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout familyListContainer;

    private String parentUid;
    private String childId; // current logged-in child

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        familyListContainer = findViewById(R.id.familyListContainer);

        // 1) Try to get parentUid from FirebaseAuth (parent navigating into child view)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            parentUid = currentUser.getUid();
        }

        // 2) If not available, try from Intent (real child login flow)
        if (parentUid == null) {
            parentUid = getIntent().getStringExtra("PARENT_UID");
        }

        // 3) Child id should always come from Intent
        childId = getIntent().getStringExtra("CHILD_ID");

        if (parentUid == null || childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Missing parent or child id for family view.", Toast.LENGTH_SHORT).show();
            return;
        }

        loadChildrenFromFirestore();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_child_family;
    }

    private void loadChildrenFromFirestore() {
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    familyListContainer.removeAllViews();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String thisChildId = doc.getId();
                        String name = doc.getString("name");

                        // Mark the current child as "You", others as "Sibling"
                        String roleLabel;
                        if (thisChildId.equals(childId)) {
                            roleLabel = "You";
                        } else {
                            roleLabel = "Sibling";
                        }

                        addFamilyRow(name, roleLabel);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load family members.", Toast.LENGTH_SHORT).show()
                );
    }

    private void addFamilyRow(String name, String role) {
        // Reuse the same row layout as parent
        android.view.View rowView = getLayoutInflater().inflate(
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

        // No click listener here: child cannot switch into another account
        familyListContainer.addView(rowView);
    }

    // ---------------- Bottom navigation ----------------

    @Override
    protected void onHomeClicked() {
        Intent intent = new Intent(this, ChildHomeActivity.class);
        intent.putExtra("PARENT_UID", parentUid);
        intent.putExtra("CHILD_ID", childId);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onFamilyClicked() {
        // Already on child family screen
    }

    @Override
    protected void onEmergencyClicked() {
        Intent intent = new Intent(this, EmergencyActivity_Child.class);
        intent.putExtra("PARENT_UID", parentUid);
        intent.putExtra("CHILD_ID", childId);
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        Intent intent = new Intent(this, ChildSettingsActivity.class);
        intent.putExtra("PARENT_UID", parentUid);
        intent.putExtra("CHILD_ID", childId);
        startActivity(intent);
    }
}
