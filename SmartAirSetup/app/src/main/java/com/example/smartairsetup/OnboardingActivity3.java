package com.example.smartairsetup;

import android.app.Activity;

public class OnboardingActivity3 extends AbstractOnboarding<OnboardingActivity4> {
    @Override
    protected int getLayoutId() {
        return R.layout.activity_onboarding3;
    }

    @Override
    protected Class<? extends Activity> getPreviousActivity() {
        return OnboardingActivity2.class;
    }
    @Override
    protected Class<OnboardingActivity4> getNextActivity() {
        return OnboardingActivity4.class;
    }
}
