package com.example.smartairsetup;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class AddMedicationActivityV2 extends AppCompatActivity {

    private EditText childNameET;
    private EditText medNameET;
    private EditText purchaseDateET;
    private EditText expiryDateET;
    private NumberPicker doseCountNP;
    private EditText notesET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_medication_v2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        getTextId();
        setCancelButton();
        setAddButton();
        setDosePicker();
        setUpDate(R.id.purchaseDateET);
        setUpDate(R.id.expiryDateET);


    }

    private void getTextId(){

        childNameET = findViewById(R.id.childNameET);
        medNameET = findViewById(R.id.medNameET);
        purchaseDateET = findViewById(R.id.purchaseDateET);
        expiryDateET = findViewById(R.id.expiryDateET);
        doseCountNP = findViewById(R.id.doseCountNP);
        notesET = findViewById(R.id.notesET);

    }


    private void setCancelButton() {
        Button cancelButton = findViewById(R.id.cancelButton);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, MedicationInventoryActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setAddButton(){

        //tk set this up so it checks all fields and ensures they are proper
        //then if they aren't highlight them light red or something
        //if they are good I should send them to firebase and add a new medication
        // I am going to have to select a child...

        Button addButton = findViewById(R.id.addButton);
        if (addButton != null) {
            addButton.setOnClickListener(v -> {

                if(!validateForm()) //swap null for list of children?
                    return;

                //only get here is form is valid inputs
                Intent intent = new Intent(this, MedicationInventoryActivity.class);
                startActivity(intent);
            });


        }
    }

    private void setDosePicker(){

        NumberPicker picker = findViewById(R.id.doseCountNP);
        picker.setMinValue(0);
        //600 is a reasonable bound but want to be safe in case someone has a weird inhaler
        picker.setMaxValue(900);
        picker.setValue(200);
        picker.setWrapSelectorWheel(false);

    }

    private void setUpDate(int idOfTV) {

        EditText DateET = findViewById(idOfTV);

        //makes a pop up dialog so user can select date of purchase
        DateET.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, y, m, d) ->
                    {DateET.setText((m + 1) + "/" + d + "/" + y);
                        DateET.setError(null); //manually gets rid of error message as user technically
                        // isn't editing the editText
                    },
                    year, month, day
            );

            dialog.show();
        });

    }

    private boolean validateForm(){

        //gets values from form
        String child = childNameET.getText().toString().trim();
        String medName = medNameET.getText().toString().trim();
        String purchase = purchaseDateET.getText().toString().trim();
        String expiry = expiryDateET.getText().toString().trim();
        String notes = notesET.getText().toString().trim(); // may be empty

        boolean flag = true;
        boolean firstError = true;

        // checks Child name is filled in
        if (child.isEmpty()) {
            childNameET.setError("Please select a child"); //what does this do?
            childNameET.requestFocus();
            firstError = false; //avoids refocusing in subsequent checks
            flag = false;
        }

        //SHOULD CHECK THAT CHILD NAME IS IN DATABASE or somehow force it during selection tk
        //forcing it may be better for user tk


        // checks Medication name is filled in
        if (medName.isEmpty()) {
            medNameET.setError("Medication name is required");
            if(firstError) {
                medNameET.requestFocus();
                //firstError = false; //uncomment if new checks are added
            }
            flag = false;
        }

        // checks purchase date is selected, note the date fields are unfocusable
        // so request focus isn't run
        if (purchase.isEmpty()) {
            purchaseDateET.setError("Please select a purchase date");
            flag = false;
        }

        // checks expiry date is selected
        if (expiry.isEmpty()) {
            expiryDateET.setError("Please select an expiry date");
            flag = false;
        }
        return flag;
    }

}


