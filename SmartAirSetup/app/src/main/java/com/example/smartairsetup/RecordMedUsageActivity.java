package com.example.smartairsetup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RecordMedUsageActivity extends AppCompatActivity {



    private NumberPicker dosePicker;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String parentUid;
    private Spinner medSpinner;
    private List<String> medIds = new ArrayList<>();
    private List<String> medNames = new ArrayList<>();

    //passed values
    private int passedFeeling;
    private String childID;

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

        //get values from previous activity to pass along for logging
        Intent intent = getIntent();
        passedFeeling = intent.getIntExtra("PRE_FEELING", 0);
        childID = intent.getStringExtra("CHILD_ID");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            parentUid = mAuth.getCurrentUser().getUid();
            Log.d("DEBUG_UID", "Using parent UID = " + parentUid);
        } else {
            Toast.makeText(this,
                    "Child isn't logged in through parent",
                    Toast.LENGTH_LONG).show();
            finish(); //not sure what this will do if child logs in with their account.
        }

        setUpMedSpinner();



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

                if (medIds.isEmpty()) {
                    Toast.makeText(this, "No medications found", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(this, PrePostCheckActivity.class);
                intent.putExtra("mode", "post");
                intent.putExtra("PRE_FEELING", passedFeeling);
                intent.putExtra("TIME_STAMP", System.currentTimeMillis());
                intent.putExtra("DOSE_COUNT", dosePicker.getValue());
                intent.putExtra("MED_ID", medIds.get(medSpinner.getSelectedItemPosition()));

                startActivity(intent);
            });
        }
    }

    private void setDosePicker() {

        dosePicker = findViewById(R.id.logDoseCountNP);
        dosePicker.setMinValue(1);
        //I think 10 is a reasonable bound
        dosePicker.setMaxValue(10);
        dosePicker.setValue(1);
        dosePicker.setWrapSelectorWheel(false);

    }


    private void setUpMedSpinner() {
        medSpinner = findViewById(R.id.logMedSpinner);

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childID)
                .collection("medications")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    medNames.clear();
                    medIds.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        // store medication name
                        String name = doc.getString("name");
                        if (name == null) name = "Unnamed Med";

                        // get medications id
                        String medId = doc.getString("med_UUID");
                        if (medId == null) medId = doc.getId();  // fallback

                        //makes two parallel lists so we can easily get values from lists
                        medNames.add(name);
                        medIds.add(medId);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            medNames
                    );

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    medSpinner.setAdapter(adapter);

                })
                .addOnFailureListener(Throwable::printStackTrace);
    }



}


