package com.example.smartairsetup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public abstract class AbstractOnboarding<T extends Activity> extends AppCompatActivity {

    protected ImageView onboardingImage;
    protected TextView onboardingHeading;
    protected TextView onboardingSubtitle;
    protected TextView onboardingDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutId());

        setViews();
        setNextButton();
        setBackButton();
        setSkipButton();
        applyFade();
    }

    // ===== ABSTRACT METHODS =====
    protected abstract int getLayoutId();             // XML layout for the screen
    protected abstract Class<T> getNextActivity();    // Next Activity type

    // ===== OPTIONAL METHODS WITH DEFAULTS =====
    // By default, previous goes to MainActivity (first screen)
    protected Class<? extends Activity> getPreviousActivity() {
        return MainActivity.class;
    }

    // By default, skip goes to MainActivity
    protected Class<? extends Activity> getSkipActivity() {
        return MainActivity.class;
    }

    // ===== COMMON LOGIC =====
    private void setViews() {
        onboardingImage = findViewById(R.id.onboardingImage);
        onboardingHeading = findViewById(R.id.onboardingHeading);
        onboardingSubtitle = findViewById(R.id.onboardingSubtitle);
        onboardingDescription = findViewById(R.id.onboardingDescription);
    }

    private void setNextButton() {
        Button nextButton = findViewById(R.id.nextButton);
        if (nextButton != null && getNextActivity() != null) {
            nextButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, getNextActivity());
                startActivity(intent);
            });
        }
    }

    private void setBackButton() {
        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            Class<? extends Activity> previous = getPreviousActivity();
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, previous);
                startActivity(intent);
            });
        }
    }

    private void setSkipButton() {
        Button skipButton = findViewById(R.id.skipButton);
        if (skipButton != null) {
            Class<? extends Activity> skipTarget = getSkipActivity();
            skipButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, skipTarget);
                startActivity(intent);
            });
        }
    }

    private void applyFade() {
        FadeIn fadeInMaker = new FadeIn(this);
        fadeInMaker.generateFadeIn(
                onboardingImage,
                onboardingHeading,
                onboardingSubtitle,
                onboardingDescription
        );
    }
}
