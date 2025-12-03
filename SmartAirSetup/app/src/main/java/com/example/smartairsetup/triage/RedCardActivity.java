package com.example.smartairsetup.triage;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.child_home_ui.ChildHomeActivity;
import com.example.smartairsetup.parent_home_ui.ParentHomeActivity;
import com.example.smartairsetup.R;

public class RedCardActivity extends AppCompatActivity {

    public static final String EXTRA_PARENT_ID = "PARENT_UID";
    public static final String EXTRA_CHILD_UID = "CHILD_ID";
    public static final String EXTRA_IS_CHILD = "extra_is_child"; // 1 = child, 0 = parent

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
        setContentView(R.layout.activity_red_card);

        int isChildFlag = getIntent().getIntExtra(EXTRA_IS_CHILD, 0); // default parent
        this.isChild = isChildFlag == 1;

        this.childUid = getIntent().getStringExtra(EXTRA_CHILD_UID);
        this.parentUid = getIntent().getStringExtra(EXTRA_PARENT_ID);

        this.cantSpeakFullSentences = getIntent().getBooleanExtra(EXTRA_CANT_SPEAK, false);
        this.chestRetractions = getIntent().getBooleanExtra(EXTRA_RETRACTIONS, false);
        this.blueLipsNails = getIntent().getBooleanExtra(EXTRA_BLUE_LIPS, false);

        if (parentUid == null || childUid == null) {
            Toast.makeText(this, "Missing parent or child UID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textTimer = findViewById(R.id.buttonStartTimer);
        Button feelWorse = findViewById(R.id.buttonFeelWorse);
        Button backButton = findViewById(R.id.backButton);
        Button nextButton = findViewById(R.id.nextButton);

        feelWorse.setOnClickListener(v -> goToEmergency());
        backButton.setOnClickListener(v -> goBackToOptional());
        nextButton.setOnClickListener(v -> {
            cancelTimer();
            Intent intent;
            if (isChild) {
                intent = new Intent(this, ChildHomeActivity.class);
            } else {
                intent = new Intent(this, ParentHomeActivity.class);
            }
            intent.putExtra(EXTRA_PARENT_ID, parentUid);
            intent.putExtra(EXTRA_CHILD_UID, childUid);
            startActivity(intent);
            finish();
        });

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
                Intent i = new Intent(RedCardActivity.this, RecheckActivity.class);
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
        Intent i = new Intent(this, isChild ? OptionalDataActivity_Child.class : OptionalDataActivity.class);
        sendCommonData(i);
        startActivity(i);
        finish();
    }

    private void sendCommonData(Intent i) {
        i.putExtra(EXTRA_PARENT_ID, parentUid);
        i.putExtra(EXTRA_CHILD_UID, childUid);
        i.putExtra(EXTRA_IS_CHILD, isChild ? 1 : 0); // send int now
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
