package com.example.smartairsetup;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.WindowDecorActionBar;

import java.util.HashMap;

public class PEFActivity extends AppCompatActivity {

    //TODO: Add firebase implementation from Aydin's
    private HashMap<String, StorageChild> childPEFMap = new HashMap<>();
    private Button chooseChildButton;
    private EditText dailyPEFInput, preMedicationPB, postMedicationPB;

    private int parsePEF(EditText editText) {
        String text = editText.getText().toString();
        int value = 0;
        if(!text.isEmpty()) {
            try {
                value = Integer.parseInt(text);
            } catch(NumberFormatException e) {
                value = -1;
            }
        }
        return Math.max(-1, value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pef);

        chooseChildButton = findViewById(R.id.chooseChildButton);
        dailyPEFInput = findViewById(R.id.dailyPEFInput);
        preMedicationPB = findViewById(R.id.preMedicationPB);
        postMedicationPB = findViewById(R.id.postMedicationPB);
        Button saveButton = findViewById(R.id.savePBButton);

        final String[] children = {"Alice", "Bob", "Charlie"};

        // Child selection
        chooseChildButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PEFActivity.this);
                builder.setTitle("Select a child");
                builder.setItems(children, (dialog, which) -> {
                    chooseChildButton.setText(children[which]);
                });
                builder.show();
            }
        });


        // Save button
        saveButton.setOnClickListener(v -> {
            String selectedChild = chooseChildButton.getText().toString();

            // Parse integer values from EditTexts
            int dailyPEF = parsePEF(dailyPEFInput);
            int prePEF = parsePEF(preMedicationPB);
            int postPEF = parsePEF(postMedicationPB);

            if(selectedChild.equals(getString(R.string.choose_child))) {
                Toast.makeText(PEFActivity.this, "Please choose a child", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if daily PEF is entered
            if(dailyPEF == 0) {
                Toast.makeText(PEFActivity.this, "Please enter the daily PEF", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create and store the PEFEntry
            StorageChild entry = new StorageChild(dailyPEF, prePEF, postPEF);
            childPEFMap.put(selectedChild, entry); // overrides previous entry

            Toast.makeText(PEFActivity.this, selectedChild + "'s PEF has successfully been saved", Toast.LENGTH_SHORT).show();
        });
    }
}
