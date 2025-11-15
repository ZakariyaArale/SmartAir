package com.example.smartairsetup;

import android.app.Activity;

public class OnboardingActivity2 extends AbstractOnboarding<OnboardingActivity3> {
    @Override
    protected int getLayoutId() {
        return R.layout.activity_onboarding2;
    }

    @Override
    protected Class<? extends Activity> getPreviousActivity() {
        return OnboardingActivity.class;
    }
    @Override
    protected Class<OnboardingActivity3> getNextActivity() {
        return OnboardingActivity3.class;
    }
}
