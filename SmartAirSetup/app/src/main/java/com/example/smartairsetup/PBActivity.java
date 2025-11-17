package com.example.smartairsetup;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class PBActivity extends AppCompatActivity {

    public ChildStorage pefStorage;
    public IntegerDataParse pefParser;

    public Button chooseChildButton;
    public EditText pbInput;

    private final HashMap<String, StorageChild> childPBMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pb);


        pefParser = new IntegerDataParse();
        pefStorage = new ChildStorage();

        chooseChildButton = findViewById(R.id.chooseChildButton);
        pbInput = findViewById(R.id.pbInput);
        Button saveButton = findViewById(R.id.savePBButton);


        ChildDiaglog childDiaglog = new ChildDiaglog(this);
        chooseChildButton.setOnClickListener(v -> childDiaglog.showSelectionDialog(chooseChildButton));

        saveButton.setOnClickListener(v -> savePB());
    }

    public void savePB() {
        String selectedChild = chooseChildButton.getText().toString();

        int pbValue = pefParser.parsePEF(pbInput);

        // Validation
        if (selectedChild.equals(getString(R.string.choose_child))) {
            Toast.makeText(this, "Please choose a child", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pbValue == 0) {
            Toast.makeText(this, getString(R.string.pb_hint), Toast.LENGTH_SHORT).show();
            return;
        }

        // Save PB value
        StorageChild entry = new StorageChild(pbValue, 0, 0);
        childPBMap.put(selectedChild, entry);
        pefStorage.save(selectedChild, entry);

        Toast.makeText(
                this,
                selectedChild + "'s PB has been saved successfully",
                Toast.LENGTH_SHORT
        ).show();
    }
}
