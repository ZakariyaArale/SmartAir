package com.example.smartairsetup.navigation;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;

/**
 * This abstract class serves as a true template for all Activities with a bottom navigation menu.
 * It handles view finding and delegates ALL click events to the subclasses, making it role-agnostic.
 */
public abstract class AbstractNavigation extends AppCompatActivity implements View.OnClickListener {

    protected ImageButton homeButton, familyButton, emergencyButton, settingsButton;
    protected TextView homeText, familyText, emergencyText, settingsText;

    // --- METHODS TO BE IMPLEMENTED BY SUBCLASSES ---

    @LayoutRes
    protected abstract int getLayoutResourceId();

    // The following methods force each subclass to define its own navigation logic.
    protected abstract void onHomeClicked();
    protected abstract void onFamilyClicked();
    protected abstract void onEmergencyClicked();
    protected abstract void onSettingsClicked();

    // --- ACTIVITY LIFECYCLE ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());
        setupViews();
        setupNavigationClickListeners();
    }

    // --- SETUP METHODS ---

    private void setupViews() {
        homeButton = findViewById(R.id.homeButton);
        homeText = findViewById(R.id.homeText);
        familyButton = findViewById(R.id.familyButton);
        familyText = findViewById(R.id.familyText);
        emergencyButton = findViewById(R.id.emergencyButton);
        emergencyText = findViewById(R.id.emergencyText);
        settingsButton = findViewById(R.id.settingsButton);
        settingsText = findViewById(R.id.settingsText);
    }

    private void setupNavigationClickListeners() {
        homeButton.setOnClickListener(this);
        familyButton.setOnClickListener(this);
        emergencyButton.setOnClickListener(this);
        settingsButton.setOnClickListener(this);
    }

    /**
     * This method is called when any of the navigation buttons are clicked.
     * It now delegates the action to the corresponding abstract method.
     */
    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.homeButton) {
            onHomeClicked();
        } else if (viewId == R.id.familyButton) {
            onFamilyClicked();
        } else if (viewId == R.id.emergencyButton) {
            onEmergencyClicked();
        } else if (viewId == R.id.settingsButton) {
            onSettingsClicked();
        }
    }
}
