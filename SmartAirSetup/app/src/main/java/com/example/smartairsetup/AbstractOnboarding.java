package com.example.smartairsetup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public abstract class AbstractOnboarding extends AppCompatActivity {

    protected ImageView onboardingImage;
    protected TextView onboardingHeading;
    protected TextView onboardingSubtitle;
    protected TextView onboardingDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutId());

        setViews();
        applyFade();
    }

    protected abstract int getLayoutId();

    void setViews() {
        onboardingImage = findViewById(R.id.onboardingImage);
        onboardingHeading = findViewById(R.id.onboardingHeading);
        onboardingSubtitle = findViewById(R.id.onboardingSubtitle);
        onboardingDescription = findViewById(R.id.onboardingDescription);
    }

    protected void applyFade() {
        FadeIn fadeInMaker = new FadeIn(this);
        fadeInMaker.generateFadeIn(
                onboardingImage,
                onboardingHeading,
                onboardingSubtitle,
                onboardingDescription
        );
    }
}