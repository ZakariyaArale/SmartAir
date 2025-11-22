package com.example.smartairsetup;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PBActivity extends AppCompatActivity {

    public ChildStorage pbStorage;
    public IntegerDataParse pbParser;

    public Button chooseChildButton;
    public EditText pbInput;

    private FirebaseFirestore db;
    private String parentID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pb);

        pbParser = new IntegerDataParse();
        pbStorage = new ChildStorage();

        chooseChildButton = findViewById(R.id.chooseChildButton);
        pbInput = findViewById(R.id.pbInput);
        Button saveButton = findViewById(R.id.savePBButton);

        db = FirebaseFirestore.getInstance();
        parentID = "qGVzsSb3PMaI3D0UumcwJpuMgMG2"; // replace later with FirebaseAuth.getInstance().getUid()

        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog childDiaglog = new ChildDiaglog(this, provider);

        chooseChildButton.setOnClickListener(v -> childDiaglog.showSelectionDialog(chooseChildButton));
        saveButton.setOnClickListener(v -> savePB());
    }

    public void savePB() {
        Object tag = chooseChildButton.getTag();

        if (tag == null) {
            Toast.makeText(this, "Please choose a child", Toast.LENGTH_SHORT).show();
            return;
        }

        String childUid = tag.toString();
        String childName = chooseChildButton.getText().toString();

        int newPB = pbParser.parsePEF(pbInput);

        if (newPB == 0) {
            Toast.makeText(this, "Enter a valid PB value", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageChild entry = new StorageChild(newPB, 0, 0);
        pbStorage.save(childUid, entry);

        savePBToFirebase(childUid, newPB);

        Toast.makeText(this, childName + "'s PB saved!", Toast.LENGTH_SHORT).show();
    }

    private void savePBToFirebase(String childUid, int newPB) {
        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .collection("PEF")
                .document("latest")
                .get()
                .addOnSuccessListener(latest -> {

                    long oldPB = latest.getLong("pb") != null ? latest.getLong("pb") : 0;

                    long finalPB = Math.max(oldPB, newPB);

                    Map<String, Object> data = new HashMap<>();
                    data.put("pb", finalPB);
                    data.put("timestamp", System.currentTimeMillis());

                    db.collection("users")
                            .document(parentID)
                            .collection("children")
                            .document(childUid)
                            .collection("PEF")
                            .document("latest")
                            .set(data, com.google.firebase.firestore.SetOptions.merge());
                });
    }
}