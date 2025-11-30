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

public class YellowCardActivity extends AppCompatActivity {

    public static final String EXTRA_PARENT_ID = "PARENT_UID";
    public static final String EXTRA_CHILD_UID = "childUID";
    public static final String EXTRA_IS_CHILD = "extra_is_child";

    public static final String EXTRA_CANT_SPEAK = "cantSpeakFullSentences";
    public static final String EXTRA_RETRACTIONS = "chestRetractions";
    public static final String EXTRA_BLUE_LIPS = "blueLipsNails";

    private static final long TEN_MIN_MS = 10 * 60 * 1000L;

    private boolean isChild;
    private String parentUid;
    private String childUid;

    private boolean cantSpeakFullSentences;
    private boolean chestRetractions;
    private boolean blueLipsNails;

    private CountDownTimer timer;
    private TextView textTimer;

    private ActivityResultLauncher<Intent> recheckLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yellow_card);

        // --- READ DATA FROM INTENT ---
        this.isChild = getIntent().getBooleanExtra(EXTRA_IS_CHILD, false);
        this.childUid = getIntent().getStringExtra(EXTRA_CHILD_UID);
        this.parentUid = getIntent().getStringExtra(EXTRA_PARENT_ID);

        this.cantSpeakFullSentences = getIntent().getBooleanExtra(EXTRA_CANT_SPEAK, false);
        this.chestRetractions = getIntent().getBooleanExtra(EXTRA_RETRACTIONS, false);
        this.blueLipsNails = getIntent().getBooleanExtra(EXTRA_BLUE_LIPS, false);

        // Validation
        if (parentUid == null) {
            Toast.makeText(this, "Missing parent UID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (childUid == null) {
            Toast.makeText(this, "Missing child UID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // UI
        textTimer = findViewById(R.id.buttonStartTimer);
        Button feelWorse = findViewById(R.id.buttonFeelWorse);
        Button backButton = findViewById(R.id.backButton);
        Button nextButton = findViewById(R.id.nextButton);

        feelWorse.setOnClickListener(v -> goToEmergency());
        backButton.setOnClickListener(v -> goBackToOptional());
        nextButton.setOnClickListener(v -> {
            cancelTimer();
            Intent intent = new Intent(this, ParentHomeActivity.class);
            startActivity(intent);
            finish();
        });

        // Recheck handler
        recheckLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    boolean needsEmergency = false;

                    if (result.getData() != null) {
                        needsEmergency = result.getData()
                                .getBooleanExtra(RecheckActivity.EXTRA_NEEDS_EMERGENCY, false);
                    }

                    if (needsEmergency) goToEmergency();
                }
        );

        startTenMinuteTimer();
    }

    // --- TIMER ---
    private void startTenMinuteTimer() {
        updateTimerText(TEN_MIN_MS);

        timer = new CountDownTimer(TEN_MIN_MS, 1000) {
            @Override
            public void onTick(long ms) {
                updateTimerText(ms);
            }

            @Override
            public void onFinish() {
                Intent i = new Intent(YellowCardActivity.this, RecheckActivity.class);
                sendCommonData(i);
                recheckLauncher.launch(i);
            }
        }.start();
    }

    private void updateTimerText(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        textTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    // --- BUTTON LOGIC ---
    private void goToEmergency() {
        cancelTimer();
        Intent i = new Intent(this, isChild ? RedFlagsActivity_Child.class : RedFlagsActivity.class);
        sendCommonData(i);
        startActivity(i);
        finish();
    }
    private void goBackToOptional() {
        cancelTimer();
        Intent i = new Intent(this, OptionalDataActivity.class);
        sendCommonData(i);
        startActivity(i);
        finish();
    }

    // --- UTILITY FOR PASSING COMMON DATA ---
    private void sendCommonData(Intent i) {
        i.putExtra(EXTRA_PARENT_ID, parentUid);
        i.putExtra(EXTRA_CHILD_UID, childUid);
        i.putExtra(EXTRA_CANT_SPEAK, cantSpeakFullSentences);
        i.putExtra(EXTRA_RETRACTIONS, chestRetractions);
        i.putExtra(EXTRA_BLUE_LIPS, blueLipsNails);
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
