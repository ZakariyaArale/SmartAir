package com.example.smartairsetup;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


public class PEFActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pef);

        chooseChildButton = findViewById(R.id.chooseChildButton);
        dailyPEFInput = findViewById(R.id.dailyPEFInput);
        preMedicationPB = findViewById(R.id.preMedicationPB);
        postMedicationPB = findViewById(R.id.postMedicationPB);
        Button saveButton = findViewById(R.id.savePBButton);


        });


        String selectedChild = chooseChildButton.getText().toString();


        if (selectedChild.equals(getString(R.string.choose_child))) {
            return;
        }

        if (dailyPEF == 0) {
            return;
        }

        StorageChild entry = new StorageChild(dailyPEF, prePEF, postPEF);

    }
}
