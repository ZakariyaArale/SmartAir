
package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChildHomeActivity extends AbstractNavigation {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button buttonBadges = findViewById(R.id.buttonBadges);
        buttonBadges.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, ChildBadgesActivity.class);
            startActivity(intent);
        });
    }


    private void setupShortcutClicks() {
        // go to Calculate PEF
        View.OnClickListener calculateListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(ChildHomeActivity.this, CalculatePefActivity.class);
                //startActivity(intent);

            }
        };

        // go to  Set Personal Best
        View.OnClickListener setPbListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(ChildHomeActivity.this, SetPersonalBestActivity.class);
                //startActivity(intent);
            }
        };
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_child_home;
    }

    @Override
    protected void onHomeClicked() {
        //Do nothing as we are on Home Page
    }

    @Override
    protected void onFamilyClicked() {
        Intent intent = new Intent(this, ChildFamilyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onEmergencyClicked() {
        // TODO: For a Child, this would go to EmergencyActivity
        // Intent intent = new Intent(this, EmergencyActivity.class);
        // startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        // TODO: For a Child, this would go to ChildSettingsActivity
        // Intent intent = new Intent(this, ChildSettingsActivity.class);
        // startActivity(intent);
    }
}
