package com.example.smartairsetup.triage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;

public class RecheckActivity extends AppCompatActivity {

    public static final String EXTRA_NEEDS_EMERGENCY = "extra_needs_emergency";

    private RadioGroup radioGroup;
    private TextView errorText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recheck);

        radioGroup = findViewById(R.id.radioRecheck);
        errorText = findViewById(R.id.textRecheckError);

        Button submit = findViewById(R.id.buttonRecheckSubmit);
        Button back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());

        submit.setOnClickListener(v -> submitRecheck());
    }

    private void submitRecheck() {
        errorText.setVisibility(View.GONE);

        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            errorText.setText("Please select an option.");
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        RadioButton rb = findViewById(checkedId);
        String choice = rb.getText().toString().toLowerCase();

        // If not improving OR red flag -> emergency
        boolean needsEmergency =
                choice.contains("worse") ||
                        choice.contains("red flag") ||
                        choice.contains("not improving");

        Intent data = new Intent();
        data.putExtra(EXTRA_NEEDS_EMERGENCY, needsEmergency);
        setResult(RESULT_OK, data);
        finish();
    }
}