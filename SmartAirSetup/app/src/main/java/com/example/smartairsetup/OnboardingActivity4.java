package com.example.smartairsetup;

import android.app.Activity;

public class OnboardingActivity4 extends AbstractOnboarding<MainActivity> {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_onboarding4; // XML for last onboarding screen
    }

    @Override
    protected Class<? extends Activity> getPreviousActivity() {
        return OnboardingActivity3.class;
    }

    @Override
    protected Class<MainActivity> getNextActivity() {
        return MainActivity.class; // Goes to main app
    }
}
