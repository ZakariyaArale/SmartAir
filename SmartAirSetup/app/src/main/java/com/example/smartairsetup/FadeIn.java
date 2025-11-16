package com.example.smartairsetup;

import android.app.Activity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ImageView;

public class FadeIn {
    private final Activity activity;

    public FadeIn(Activity activity) {
        this.activity = activity;
    }

    public void generateFadeIn(ImageView image, TextView heading, TextView subtitle, TextView description) {
        Animation fadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
        image.startAnimation(fadeIn);
        heading.startAnimation(fadeIn);
        subtitle.startAnimation(fadeIn);
        description.startAnimation(fadeIn);
    }
}
