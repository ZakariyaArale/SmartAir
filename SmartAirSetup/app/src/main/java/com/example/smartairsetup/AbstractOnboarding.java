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

    //Setup all functionality for the activity
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

    // For sending the screen layout for traversal
    protected abstract int getLayoutId();
    protected abstract Class<T> getNextActivity();

    //This is default behaivour TODO: fix when Mainacitivty is implemented by Aydin
    protected Class<? extends Activity> getPreviousActivity() {

        return MainActivity.class;
    }


    protected Class<? extends Activity> getSkipActivity() {

        return MainActivity.class;
    }

    // Concrete methods for setting views and buttons
    void setViews() {
        onboardingImage = findViewById(R.id.onboardingImage);
        onboardingHeading = findViewById(R.id.onboardingHeading);
        onboardingSubtitle = findViewById(R.id.onboardingSubtitle);
        onboardingDescription = findViewById(R.id.onboardingDescription);
    }

    protected void setNextButton() {
        Button nextButton = findViewById(R.id.nextButton);
        if (nextButton != null && getNextActivity() != null) {
            nextButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, getNextActivity());
                startActivity(intent);
            });
        }
    }

    protected void setBackButton() {
        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            Class<? extends Activity> previous = getPreviousActivity();
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, previous);
                startActivity(intent);
            });
        }
    }

    protected void setSkipButton() {
        Button skipButton = findViewById(R.id.skipButton);
        if (skipButton != null) {
            Class<? extends Activity> skipTarget = getSkipActivity();
            skipButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, skipTarget);
                startActivity(intent);
            });
        }
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
