package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PrePostCheckActivity extends AppCompatActivity {

    TextView checkInTitleTV;
    Button nextButton;

    RadioGroup segmentGroup;
    RadioButton opt1;
    RadioButton opt2;
    RadioButton opt3;
    RadioButton opt4;
    RadioButton opt5;
    Spinner feelingSpinner;

    int selected;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pre_post_check);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getIds();

        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");
        if(mode == null || mode.equals("post")){
            setUpPostCheck();
        }

        setBackButton();
        setNextButton();
        setSegmentGroup();

    }

    private void getIds(){

        opt1 = findViewById(R.id.opt1);
        opt2 = findViewById(R.id.opt2);
        opt3 = findViewById(R.id.opt3);
        opt4 = findViewById(R.id.opt4);
        opt5 = findViewById(R.id.opt5);
        segmentGroup = findViewById(R.id.BreathingSG);
        checkInTitleTV = findViewById(R.id.medCheckInTitleTV);
        nextButton = findViewById(R.id.checkInNextButton);

    }

    private void setUpPostCheck(){

        checkInTitleTV.setText("Post Medication Check");
        nextButton.setText("Finish");

        setUpSpinner();
        //make 2nd question visible

    }

    private void setUpSpinner(){

        feelingSpinner = findViewById(R.id.feelingSpinner);

        feelingSpinner.setVisibility(View.VISIBLE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Worse", "Same", "Better"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        feelingSpinner.setAdapter(adapter);

    }

    private void setBackButton() {
        Button backButton = findViewById(R.id.checkInBackButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                finish();
            });
        }
    }

    private void setNextButton(){
        Button nextButton = findViewById(R.id.checkInNextButton);
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {

                //log selected information here.
                /*
                my current plan would be to pass it to the next activity if in pre,
                and save it to firebase logs if in post along with the other needed data:

                Date - timestamp


                maybe put option for training in taking medication tab.
                remember we have to use this for badges.

                 */



                Intent intent = new Intent(this, AddEditMedicationActivity.class); ////Change this to next page
                intent.putExtra("mode", "new");
                startActivity(intent);
            });
        }

    }

    private void setSegmentGroup(){

        segmentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            selected = 0;
            if(opt1.isSelected()){selected = 1;}
            else if(opt2.isSelected()){selected = 2;}
            else if(opt3.isSelected()){selected = 3;}
            else if(opt4.isSelected()){selected = 4;}
            else if(opt5.isSelected()){selected = 5;}
            nextButton.setEnabled(true);

        });


    }


}