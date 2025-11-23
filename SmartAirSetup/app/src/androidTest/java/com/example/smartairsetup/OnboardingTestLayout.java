package com.example.smartairsetup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;


//In short this is just testing if all classes return the correct layout and screens for traversal
@RunWith(AndroidJUnit4.class)
public class OnboardingTestLayout {
/*
    @Test
    public void testOnboardingActivity1() {
        ActivityScenario<OnboardingActivity> scenario =
                ActivityScenario.launch(OnboardingActivity.class);

        scenario.onActivity(activity -> {
            assertEquals(R.layout.activity_onboarding, activity.getLayoutId());
            assertEquals(OnboardingActivity2.class, activity.getNextActivity());
            assertEquals(MainActivity.class, activity.getPreviousActivity());
            assertEquals(MainActivity.class, activity.getSkipActivity());
        });
    }

    @Test
    public void testOnboardingActivity2() {
        ActivityScenario<OnboardingActivity2> scenario =
                ActivityScenario.launch(OnboardingActivity2.class);

        scenario.onActivity(activity -> {
            assertEquals(R.layout.activity_onboarding2, activity.getLayoutId());
            assertEquals(OnboardingActivity3.class, activity.getNextActivity());
            assertEquals(OnboardingActivity.class, activity.getPreviousActivity());
            assertEquals(MainActivity.class, activity.getSkipActivity());
        });
    }

    @Test
    public void testOnboardingActivity3() {
        ActivityScenario<OnboardingActivity3> scenario =
                ActivityScenario.launch(OnboardingActivity3.class);

        scenario.onActivity(activity -> {
            assertEquals(R.layout.activity_onboarding3, activity.getLayoutId());
            assertEquals(OnboardingActivity4.class, activity.getNextActivity());
            assertEquals(OnboardingActivity2.class, activity.getPreviousActivity());
            assertEquals(MainActivity.class, activity.getSkipActivity());
        });
    }

    @Test
    public void testOnboardingActivity4() {
        ActivityScenario<OnboardingActivity4> scenario =
                ActivityScenario.launch(OnboardingActivity4.class);

        scenario.onActivity(activity -> {
            assertEquals(R.layout.activity_onboarding4, activity.getLayoutId());
            assertEquals(MainActivity.class, activity.getNextActivity());
            assertEquals(OnboardingActivity3.class, activity.getPreviousActivity());
            assertEquals(MainActivity.class, activity.getSkipActivity());
        });
    }

 */
}