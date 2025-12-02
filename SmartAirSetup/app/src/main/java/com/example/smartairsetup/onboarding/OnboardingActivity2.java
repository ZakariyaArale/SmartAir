package com.example.smartairsetup.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.smartairsetup.R;
import com.example.smartairsetup.child_home_ui.ChildHomeActivity;
import com.example.smartairsetup.login.SignupActivity;

public class OnboardingActivity2 extends AbstractOnboarding {

    private String parentUid;
    private String childId;
    private boolean firstTime;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_onboarding2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve onboarding data (same pattern as Activity 1)
        parentUid = getIntent().getStringExtra("PARENT_UID");
        childId = getIntent().getStringExtra("CHILD_ID");
        firstTime = getIntent().getBooleanExtra("firstTime", false);

        setNextButton();
        setBackButton();
        setSkipButton();
    }

    private void setNextButton() {
        Button nextButton = findViewById(R.id.nextButton);
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {

                Intent intent = new Intent(this, OnboardingActivity3.class);

                // Pass child onboarding data forward
                if (parentUid != null) intent.putExtra("PARENT_UID", parentUid);
                if (childId != null) intent.putExtra("CHILD_ID", childId);
                intent.putExtra("firstTime", firstTime);

                startActivity(intent);
            });
        }
    }

    private void setBackButton() {
        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {

                Intent intent = new Intent(this, OnboardingActivity.class);

                // Send child fields BACKWARD
                if (parentUid != null) intent.putExtra("PARENT_UID", parentUid);
                if (childId != null) intent.putExtra("CHILD_ID", childId);
                intent.putExtra("firstTime", firstTime);

                startActivity(intent);
            });
        }
    }

    private void setSkipButton() {
        Button skipButton = findViewById(R.id.skipButton);
        if (skipButton != null) {
            skipButton.setOnClickListener(v -> {

                // If this is a child onboarding, skip → ChildHomeActivity
                if (parentUid != null && childId != null) {
                    Intent intent = new Intent(this, ChildHomeActivity.class);
                    intent.putExtra("PARENT_UID", parentUid);
                    intent.putExtra("CHILD_ID", childId);
                    startActivity(intent);
                    finish();
                    return;
                }

                // Otherwise fallback to original skip
                Intent intent = new Intent(this, SignupActivity.class);
                startActivity(intent);
            });
        }
    }
}