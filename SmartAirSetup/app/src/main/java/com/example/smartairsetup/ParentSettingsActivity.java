package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ParentSettingsActivity extends AbstractNavigation {

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
        //TODO: Sign Out Should be Implemeneted
        });
    }



    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_parent_settings;
    }

    @Override
    protected void onHomeClicked() {
        Intent intent = new Intent(ParentSettingsActivity.this, ParentHomeActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onFamilyClicked() {
        Intent intent = new Intent(ParentSettingsActivity.this, ParentFamilyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onEmergencyClicked() {
        Intent intent = new Intent(ParentSettingsActivity.this, EmergencyActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        //Dont do anything since we are in settings
    }
}
