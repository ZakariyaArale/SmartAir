package com.example.smartairsetup;

public class OnboardingActivity extends AbstractOnboarding<OnboardingActivity2> {
    @Override
    protected int getLayoutId() {
        return R.layout.activity_onboarding;
    }

    //TODO: When login is made for new user linked to here
    @Override
    protected Class<OnboardingActivity2> getNextActivity() {
        return OnboardingActivity2.class;
    }
}
