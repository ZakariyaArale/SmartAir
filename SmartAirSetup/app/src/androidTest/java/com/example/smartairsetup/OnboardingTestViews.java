package com.example.smartairsetup;

import android.widget.Button;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
/*For this we're testing if the system doesn't crash and all the views exist
* also OnboardingActivity (), 2, 3, 4 are all instances of abstract so for the view
* its only necessary to test only OnboardingActivity
 */
public class OnboardingTestViews {

    //Set up activity rule to launch the activity before each test
    @Rule
    public ActivityScenarioRule<OnboardingActivity> activityRule =
            new ActivityScenarioRule<>(OnboardingActivity.class);

    @Test
    //Test if views exist for images and text
    public void testSetViewsNotNull() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.onboardingImage);
            assertNotNull(activity.onboardingHeading);
            assertNotNull(activity.onboardingSubtitle);
            assertNotNull(activity.onboardingDescription);
        });
    }

    @Test
    //Test if views exist for buttons
    public void testViewButtons() {
        activityRule.getScenario().onActivity(activity -> {
            activity.setNextButton();
            activity.setBackButton();
            activity.setSkipButton();

            Button next = activity.findViewById(R.id.nextButton);
            Button back = activity.findViewById(R.id.backButton);
            Button skip = activity.findViewById(R.id.skipButton);

            assertNotNull(next);
            assertNotNull(back);
            assertNotNull(skip);

            assertTrue(next.isEnabled());
            assertTrue(back.isEnabled());
            assertTrue(skip.isEnabled());
        });
    }

    @Test
    //Test if fade animation doesn't crash app
    public void testApplyFade() {
        activityRule.getScenario().onActivity(activity -> {
            activity.applyFade();

            //Doine a bit of research this is require to test if any animation doesn't crash app
            assertTrue(activity.onboardingImage.getAlpha() >= 0f && activity.onboardingImage.getAlpha() <= 1f);
            assertTrue(activity.onboardingHeading.getAlpha() >= 0f && activity.onboardingHeading.getAlpha() <= 1f);
            assertTrue(activity.onboardingSubtitle.getAlpha() >= 0f && activity.onboardingSubtitle.getAlpha() <= 1f);
            assertTrue(activity.onboardingDescription.getAlpha() >= 0f && activity.onboardingDescription.getAlpha() <= 1f);
        });
    }


}