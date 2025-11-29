package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChildHomeActivity extends AbstractNavigation {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView greetingText;
    private String parentUid;
    private String childId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        parentUid = currentUser.getUid();

        Intent intent = getIntent();
        if (intent != null) {
            childId = intent.getStringExtra("CHILD_ID");
        }
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Child id is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        greetingText = findViewById(R.id.greetingText);

        // Only load child data here
        loadChild();

    }



    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_child_home;
    }

    // 1) Load child document from Firestore
    private void loadChild() {
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        setGreeting(name);
                        setButtons();
                    } else {
                        Toast.makeText(this, "Child not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load child.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setButtons(){
        ImageButton buttonBadges = findViewById(R.id.buttonBadges);
        buttonBadges.setOnClickListener(v -> {
            if (childId.isEmpty()) {
                Toast.makeText(
                        ChildHomeActivity.this,
                        "Please add a child first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            Intent new_intent = new Intent(ChildHomeActivity.this, ChildBadgesActivity.class);
            startActivity(new_intent);
            new_intent.putExtra("CHILD_ID", childId);
        });
    }

    // 2) Only set greeting message here
    private void setGreeting(String name) {
        if (name != null && !name.isEmpty()) {
            String message = "Hi, " + name;
            greetingText.setText(message);
        } else {
            Toast.makeText(this, "Child name is empty.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onHomeClicked() {
        Intent intent = new Intent(this, ParentHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onFamilyClicked() {
        Intent intent = new Intent(this, ParentFamilyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onEmergencyClicked() {
        Intent intent = new Intent(this, EmergencyActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        Intent intent = new Intent(ChildHomeActivity.this, ChildSettingsActivity.class);
        startActivity(intent);
    }
}
