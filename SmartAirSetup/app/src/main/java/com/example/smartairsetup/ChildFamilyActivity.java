package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;

// 1. Extend from AbstractNavigation instead of AppCompatActivitypublic class ParentHomeActivity extends AbstractNavigation {
public class ChildFamilyActivity extends AbstractNavigation {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_child_family;
    }

    @Override
    protected void onHomeClicked() {
        Intent intent = new Intent(this, ChildHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onFamilyClicked() {
        //Do nothing as we are on Family Page
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
