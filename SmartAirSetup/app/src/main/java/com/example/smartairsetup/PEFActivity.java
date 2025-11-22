package com.example.smartairsetup;
// PARENT ONLY PARENT ONLY

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PEFActivity extends AppCompatActivity {

    public ChildStorage pefStorage;
    public IntegerDataParse pefParser;

    public Button chooseChildButton;
    public EditText dailyPEFInput;
    public EditText preMedicationPB;
    public EditText postMedicationPB;

    private FirebaseFirestore db;
    private String parentID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pef);

        pefParser = new IntegerDataParse();
        pefStorage = new ChildStorage();

        chooseChildButton = findViewById(R.id.chooseChildButton);
        dailyPEFInput = findViewById(R.id.dailyPEFInput);
        preMedicationPB = findViewById(R.id.preMedicationPB);
        postMedicationPB = findViewById(R.id.postMedicationPB);
        Button saveButton = findViewById(R.id.savePBButton);

        db = FirebaseFirestore.getInstance();
        parentID = "qGVzsSb3PMaI3D0UumcwJpuMgMG2";

        // Child selection dialog
        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog childDiaglog = new ChildDiaglog(this, provider);

        chooseChildButton.setOnClickListener(v ->
                childDiaglog.showSelectionDialog(chooseChildButton));

        saveButton.setOnClickListener(v -> savePEF());
    }

    public void savePEF() {
        Object tag = chooseChildButton.getTag();

        if (tag == null) {
            Toast.makeText(this, "Please choose a child", Toast.LENGTH_SHORT).show();
            return;
        }

        String childUid = tag.toString();

        int dailyPEF = pefParser.parsePEF(dailyPEFInput);
        int prePEF = pefParser.parsePEF(preMedicationPB);
        int postPEF = pefParser.parsePEF(postMedicationPB);

        if (dailyPEF == 0) {
            Toast.makeText(this, "Please enter the daily PEF", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageChild entry = new StorageChild(dailyPEF, prePEF, postPEF);
        pefStorage.save(childUid, entry);

        saveChildPEFToFirebase(childUid, entry);
    }

    private void saveChildPEFToFirebase(String childUid, StorageChild entry) {

        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childUid)
                .collection("PEF")
                .document("latest")
                .get()
                .addOnSuccessListener(latestDoc -> {

                    long oldDaily = latestDoc.getLong("dailyPEF") != null ? latestDoc.getLong("dailyPEF") : 0;
                    long oldPre = latestDoc.getLong("prePEF") != null ? latestDoc.getLong("prePEF") : 0;
                    long oldPost = latestDoc.getLong("postPEF") != null ? latestDoc.getLong("postPEF") : 0;

                    long finalDaily = Math.max(oldDaily, entry.getDailyPEF());
                    long finalPre = Math.max(oldPre, entry.getPrePEF());
                    long finalPost = Math.max(oldPost, entry.getPostPEF());

                    Map<String, Object> data = new HashMap<>();
                    data.put("dailyPEF", finalDaily);
                    data.put("prePEF", finalPre);
                    data.put("postPEF", finalPost);

                    db.collection("users")
                            .document(parentID)
                            .collection("children")
                            .document(childUid)
                            .collection("PEF")
                            .document("latest")
                            .set(data)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "PEF saved successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Log.e("PEFActivity", "Error saving PEF", e));
                });
    }
}