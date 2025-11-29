package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecordMedUsageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_record_med_usage);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setBackButton();
        setNextButton();
        setDosePicker();

    }

    private void setBackButton() {
        Button backButton = findViewById(R.id.medLogBackButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                finish();
            });
        }
    }

    private void setNextButton() {
        Button backButton = findViewById(R.id.medLogNextButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, PrePostCheckActivity.class); ////Change this to next page
                intent.putExtra("mode", "post");
                startActivity(intent);
            });
        }
    }

    private void setDosePicker() {

        NumberPicker picker = findViewById(R.id.logDoseCountNP);
        picker.setMinValue(1);
        //I think 10 is a reasonable bound, as
        picker.setMaxValue(10);
        picker.setValue(1);
        picker.setWrapSelectorWheel(false);

    }



}