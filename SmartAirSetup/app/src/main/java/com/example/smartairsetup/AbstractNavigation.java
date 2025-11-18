package com.example.smartairsetup;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

//TO DO: This abstract class will be used as a template to navigate
// between home, family, profile, settings screens

public abstract class AbstractNavigation <T extends Activity> extends AppCompatActivity {
//    protected ImageButton homeButton;
//    protected ImageButton familyButton;
//    protected ImageButton profileButton;
//    protected ImageButton settingsButton;

    //Setup all functionality for the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setViews();
//        setHomeButton();
//        setFamilyButton();
//        setProfileButton();
//        setSettingsButton();
    }

    void setViews() {
//        homeButton = findViewById(R.id.homeButton);
//        familyButton = findViewById(R.id.familyButton);
//        profileButton = findViewById(R.id.profileButton);
//        settingsButton = findViewById(R.id.settingsButton);
    }

    //protected void setHomeButton();

    //protected void setFamilyButton();
    //protected void setProfileButton();
    //protected void setSettingsButton();
}
