package com.example.smartairsetup;

import android.app.Activity;

public class OnboardingActivity4 extends AbstractOnboarding<MainActivity> {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_onboarding4;
    }

    @Override
    protected Class<? extends Activity> getPreviousActivity() {
        return OnboardingActivity3.class;
    }

    //TODO: Fix when main menus are fixed
    @Override
    protected Class<MainActivity> getNextActivity() {
        return MainActivity.class;
    }
}
