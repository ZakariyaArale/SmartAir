package com.example.smartairsetup.technique;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartairsetup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TechniqueTraining extends AppCompatActivity {

    private FirebaseFirestore db;
    private  String parentUid;
    private String childId;
    private int promptCount;
    private int techniqueScore;
    private boolean mask;
    private boolean continueMode;
    private boolean goodZone;
    private CountDownTimer goodTimer;
    private Button inhalerVideoButton;
    private Button finishButton;
    private Button promptButton;
    private TextView promptText;
    private Button maskHintButton;
    private Button yesButton;
    private Button noButton;
    private Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_technique_training);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            parentUid = mAuth.getCurrentUser().getUid();
        } else {
            finish(); // Should never happen, but safe
        }

        Intent intent = getIntent();
        childId = intent.getStringExtra("CHILD_ID");

        setIds();
        setVideoButton();
        setFinishButton();
        setPromptButton();
        setMaskHintButton();
        setNoButton();
        setYesButton();
        setExitButton();

        promptCount = 0;
        techniqueScore = 0;
        mask = false; //tracks whether user wants mask hints
        continueMode = false; //used in setYesButton
        goodZone = false; //used to check how long user waits between doses
        // (decides how good technique was, I wasn't sure how else to do this)

    }


    private void setIds(){

        inhalerVideoButton = findViewById(R.id.videoButton);
        finishButton = findViewById(R.id.techniqueFinishButton);
        promptButton = findViewById(R.id.nextPromptButton);
        promptText = findViewById(R.id.promptTV);
        maskHintButton = findViewById(R.id.maskTipsButton);
        yesButton = findViewById(R.id.yesButton);
        noButton = findViewById(R.id.noButton);
        exitButton = findViewById(R.id.techniqueExitButton);

    }

    public void setExitButton() {
        if (exitButton != null) {
            exitButton.setOnClickListener(v -> finish());
        }
    }

    private void setVideoButton(){

        if (inhalerVideoButton != null) {
            inhalerVideoButton.setOnClickListener(v -> {

                Intent intent = new Intent(this, TechniqueTrainingVideo.class);
                startActivity(intent);

            });
        }
    }

    private void setFinishButton(){

        if (finishButton != null) {
            finishButton.setOnClickListener(v -> {

                // Build log entry
                Map<String, Object> techniqueLogs = new HashMap<>();
                techniqueLogs.put("timestamp", System.currentTimeMillis());
                techniqueLogs.put("childId", childId);

                if(promptCount < 7){ //if technique training wasn't fully completed
                    techniqueLogs.put("quality", "low");
                }else if(techniqueScore == 0){
                    techniqueLogs.put("quality", "high");
                }else{
                    techniqueLogs.put("quality", "average");
                }

                db.collection("users")
                        .document(parentUid)
                        .collection("children")
                        .document(childId)
                        .collection("techniqueLogs")
                        .add(techniqueLogs)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(this, "technique log saved!", Toast.LENGTH_SHORT).show();
                            finish(); // go back or go somewhere else
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error saving log", Toast.LENGTH_SHORT).show());

            });
        }
    }

    private void setMaskHintButton(){

        if (maskHintButton != null) {
            maskHintButton.setOnClickListener(v -> {

                hideYesNo();

                promptCount = 0;
                techniqueScore = 0;
                if (mask) {
                    mask = false;
                    maskHintButton.setText(R.string.tech_mask_tips_button);
                } else {
                    mask = true;
                    maskHintButton.setText(R.string.tech_mask_basic_inhaler_button);
                }
                updatePrompt();

            });
        }
    }

    public void setNoButton() {
        if (noButton != null) {
            noButton.setOnClickListener(v -> {

                hideYesNo();
                promptCount++;
                updatePrompt();

            });
        }
    }

    public void setYesButton() {

        if (yesButton != null) {
            yesButton.setOnClickListener(v -> {

                if (!continueMode) {

                    noButton.setVisibility(TextView.INVISIBLE);
                    yesButton.setText(R.string.tech_continue);
                    continueMode = true;
                    promptText.setText(R.string.tech_wait_between_doses);
                    startGoodTimer();

                } else {
                    if (!goodZone) {
                        // user didn't continue in the 30 - 60 second window
                        techniqueScore--;
                    }
                    continueMode = false;
                    yesButton.setText(R.string.tech_yes);
                    promptCount = 0;
                    hideYesNo();
                    updatePrompt();
                }

            });
        }
    }

    private void startGoodTimer(){

        if(goodTimer != null){
            goodTimer.cancel();
            goodZone = false;
        }

        goodTimer = new CountDownTimer(60000, 1000) {
            //safety check in case user flips through whole process in less than 30 seconds
            public void onTick(long millisUntilFinished) {
                if(millisUntilFinished <= 30000){ // at 30 seconds the good time window starts
                    goodZone = true;
                }
            }
            public void onFinish() {
                goodZone = false;
            }
        }.start();
    }

    private void setPromptButton(){

        if (promptButton != null) {
            promptButton.setOnClickListener(v -> {

                promptCount++;
                updatePrompt();

            });
        }
    }

    private void updatePrompt(){
        if(!mask) {

            if (promptCount == 0) {
                promptText.setText(R.string.tech_step1_no_mask);
            } else if (promptCount == 1) {
                promptText.setText(R.string.tech_step2_no_mask);
            } else if (promptCount == 2) {
                promptText.setText(R.string.tech_step3_no_mask);
            } else if (promptCount == 3) {
                promptText.setText(R.string.tech_step4_no_mask);
            } else if (promptCount == 4) {
                promptText.setText(R.string.tech_step5_no_mask);

            } else if (promptCount == 5) {
                promptText.setText(R.string.tech_is_another_dose_prescribed);
                yesButton.setVisibility(TextView.VISIBLE);
                noButton.setVisibility(TextView.VISIBLE);
                promptButton.setVisibility(TextView.INVISIBLE);


            } else if (promptCount == 6) {
                promptText.setText(R.string.tech_step6_no_mask);

            } else if (promptCount == 7) {
                promptText.setText(R.string.tech_finished);
            }
        } else {

            if (promptCount == 0) {
                promptText.setText(R.string.tech_step1_mask);
            } else if (promptCount == 1) {
                promptText.setText(R.string.tech_step2_mask);
            } else if (promptCount == 2) {
                promptText.setText(R.string.tech_step3_mask);
            } else if (promptCount == 3) {
                promptText.setText(R.string.tech_step4_mask);
            } else if (promptCount == 4) {
                // same behaviour: just skip to the next prompt
                promptCount++;
                updatePrompt();
            } else if (promptCount == 5) {
                promptText.setText(R.string.tech_is_another_dose_prescribed);
                yesButton.setVisibility(TextView.VISIBLE);
                noButton.setVisibility(TextView.VISIBLE);
                promptButton.setVisibility(TextView.INVISIBLE);
            } else if (promptCount == 6) {
                promptText.setText(R.string.tech_step6_mask);
            } else if (promptCount == 7) {
                // reuse the same finish text as non-mask branch
                promptText.setText(R.string.tech_finished);
            }
        }
    }

    private void hideYesNo(){
        yesButton.setVisibility(TextView.INVISIBLE);
        noButton.setVisibility(TextView.INVISIBLE);
        promptButton.setVisibility(TextView.VISIBLE);
    }

}