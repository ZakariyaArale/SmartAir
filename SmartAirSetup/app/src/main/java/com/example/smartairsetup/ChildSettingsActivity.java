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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button buttonSignOut = findViewById(R.id.buttonSignOut);
        buttonSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, MainActivity.class);
            //Make sure the user cannot go back to the main screen after signing out
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
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
        startActivity(intent);
    }

    @Override
    protected void onFamilyClicked() {
        Intent intent = new Intent(ChildSettingsActivity.this, ChildFamilyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onEmergencyClicked() {
        Intent intent = new Intent(ChildSettingsActivity.this, EmergencyActivity_Child.class);
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        //Dont do anything since we are in settings
    }
}
