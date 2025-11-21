package com.example.smartairsetup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class OnboardingActivity3 extends AbstractOnboarding {
    @Override
    protected int getLayoutId() {
        return R.layout.activity_onboarding3;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setNextButton();
        setBackButton();
        setSkipButton();
    }

    private void setNextButton() {
        Button nextButton = findViewById(R.id.nextButton);
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, OnboardingActivity4.class);
                startActivity(intent);
            });
        }
    }

    private void setBackButton() {
        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, OnboardingActivity2.class);
                startActivity(intent);
            });
        }
    }

    private void setSkipButton() {
        Button skipButton = findViewById(R.id.skipButton);
        if (skipButton != null) {
            skipButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, SignupActivity.class);
                startActivity(intent);
            });
        }
    }
}