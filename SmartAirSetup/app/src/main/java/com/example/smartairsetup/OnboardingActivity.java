package com.example.smartairsetup;

public class OnboardingActivity extends AbstractOnboarding<OnboardingActivity2> {
    @Override
    protected int getLayoutId() {
        return R.layout.activity_onboarding;
    }

    @Override
    protected Class<OnboardingActivity2> getNextActivity() {
        return OnboardingActivity2.class;
    }
}
