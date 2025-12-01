package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class GreenCardActivity extends AppCompatActivity {

    public static final String EXTRA_PARENT_ID = "extra_parent_id";

    public static final String EXTRA_IS_CHILD = "extra_is_child";
    public static final String EXTRA_CHILD_UID = "extra_child_uid";

    private static final long TEN_MIN_MS = 10 * 60 * 1000L;

    private boolean isChild;
    private String childUid;

    private CountDownTimer timer;
    private TextView textTimer;

    private ActivityResultLauncher<Intent> recheckLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_green_card);
        boolean isChild = getIntent().getBooleanExtra(EXTRA_IS_CHILD, false);
        String childUid = getIntent().getStringExtra(EXTRA_CHILD_UID);
        String parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);

        // Fallback for testing if parentId wasn’t passed
        if (parentId == null || parentId.trim().isEmpty()) {
            parentId = "VfB95gwXXyWFAqdajTHJBgyeYfB3";
        }

        // Quick sanity check to avoid null crashes
        if (childUid == null || childUid.trim().isEmpty()) {
            Toast.makeText(this, "Missing child info", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        isChild = getIntent().getBooleanExtra(EXTRA_IS_CHILD, false);
        childUid = getIntent().getStringExtra(EXTRA_CHILD_UID);

        textTimer = findViewById(R.id.buttonStartTimer);              // add to XML
        Button feelWorse = findViewById(R.id.buttonFeelWorse); // add to XML

        feelWorse.setOnClickListener(v -> goToEmergency());

        recheckLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    boolean needsEmergency = false;
                    if (result.getData() != null) {
                        needsEmergency = result.getData()
                                .getBooleanExtra(RecheckActivity.EXTRA_NEEDS_EMERGENCY, false);
                    }
                    if (needsEmergency) goToEmergency();
                    else goBackToZone();
                }
        );

        startTenMinuteTimer();
    }

    private void startTenMinuteTimer() {
        updateTimerText(TEN_MIN_MS);

        timer = new CountDownTimer(TEN_MIN_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                // pop up the re-check “screen”
                Intent i = new Intent(GreenCardActivity.this, RecheckActivity.class);
                recheckLauncher.launch(i);
            }
        }.start();
    }

    private void updateTimerText(long ms) {
        long totalSeconds = ms / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        textTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void goToEmergency() {
        cancelTimer();

        Intent i = new Intent(this, isChild ? EmergencyActivity_Child.class : EmergencyActivity.class);
        if (childUid != null) i.putExtra(EXTRA_CHILD_UID, childUid); // pass-through if you need it
        startActivity(i);
        finish();
    }

    private void goBackToZone() {
        cancelTimer();

        Intent i = new Intent(this, isChild ? ZoneActivityChild.class : ZoneActivity.class);
        if (childUid != null) i.putExtra(EXTRA_CHILD_UID, childUid);
        startActivity(i);
        finish();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelTimer();
        super.onDestroy();
    }
}