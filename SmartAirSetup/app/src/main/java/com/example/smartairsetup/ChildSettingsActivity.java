package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class ChildSettingsActivity extends AbstractNavigation {

    // We keep the child id here so we can pass it back when navigating
    private String childId;
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        // This view id ("main") should be the root view in activity_child_settings.xml
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Read child id from the intent that opened ChildSettingsActivity
        Intent intent = getIntent();
        if (intent != null) {
            childId = intent.getStringExtra("CHILD_ID");
            parentId = intent.getStringExtra("PARENT_UID");
        }

        Button buttonSignOut = findViewById(R.id.buttonSignOut);
        buttonSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent signOutIntent = new Intent(this, MainActivity.class);
            // Make sure the user cannot go back to the main screen after signing out
            signOutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(signOutIntent);
            finish();
        });
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_child_settings;
    }

    @Override
    protected void onHomeClicked() {
        Intent intent = new Intent(ChildSettingsActivity.this, ChildHomeActivity.class);
        if (childId != null && !childId.isEmpty()) {
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra("PARENT_UID", parentId);
        }
        startActivity(intent);
    }

    @Override
    protected void onFamilyClicked() {
        Intent intent = new Intent(ChildSettingsActivity.this, ChildFamilyActivity.class);
        if (childId != null && !childId.isEmpty()) {
            intent.putExtra("CHILD_ID", childId);
        }
        startActivity(intent);
    }

    @Override
    protected void onEmergencyClicked() {
        Intent intent = new Intent(ChildSettingsActivity.this, EmergencyActivity_Child.class);
        if (childId != null && !childId.isEmpty()) {
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra("PARENT_UID", parentId);
        }
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        // Already in settings; do nothing
    }
}
