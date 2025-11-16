package com.example.smartairsetup;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class PEFActivity extends AppCompatActivity {

    // Fields used by tests
    public ChildStorage pefStorage;
    public IntegerDataParse pefParser;

    public Button chooseChildButton;
    public EditText dailyPEFInput;
    public EditText preMedicationPB;
    public EditText postMedicationPB;

    private final HashMap<String, StorageChild> childPEFMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pef);

        // Initialize parser and storage
        pefParser = new IntegerDataParse();
        pefStorage = new ChildStorage();

        chooseChildButton = findViewById(R.id.chooseChildButton);
        dailyPEFInput = findViewById(R.id.dailyPEFInput);
        preMedicationPB = findViewById(R.id.preMedicationPB);
        postMedicationPB = findViewById(R.id.postMedicationPB);
        Button saveButton = findViewById(R.id.savePBButton);

        // Use ChildDiaglog for child selection
        ChildDiaglog childDiaglog = new ChildDiaglog(this);
        chooseChildButton.setOnClickListener(v -> childDiaglog.showSelectionDialog(chooseChildButton));

        saveButton.setOnClickListener(v -> savePEF());
    }

    // Public method used in tests
    public void savePEF() {
        String selectedChild = chooseChildButton.getText().toString();

        int dailyPEF = pefParser.parsePEF(dailyPEFInput);
        int prePEF = pefParser.parsePEF(preMedicationPB);
        int postPEF = pefParser.parsePEF(postMedicationPB);

        // Validation
        if (selectedChild.equals(getString(R.string.choose_child))) {
            Toast.makeText(this, "Please choose a child", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dailyPEF == 0) {
            Toast.makeText(this, "Please enter the daily PEF", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and save entry
        StorageChild entry = new StorageChild(dailyPEF, prePEF, postPEF);
        childPEFMap.put(selectedChild, entry);
        pefStorage.save(selectedChild, entry);

        Toast.makeText(this,
                selectedChild + "'s PEF has been saved successfully",
                Toast.LENGTH_SHORT).show();
    }
}
