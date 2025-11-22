package com.example.smartairsetup;
// PARENT ONLY PARENT ONLY

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            parentID = auth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Child selection dialog
        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog childDiaglog = new ChildDiaglog(this, provider);

        chooseChildButton.setOnClickListener(v ->
                childDiaglog.showSelectionDialog(chooseChildButton));

        saveButton.setOnClickListener(v -> savePEF());

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    @androidx.annotation.Nullable
    private String computeZone(long dailyPEF, long pb) {
        if (dailyPEF <= 0 || pb <= 0) return null;
        double percentage = (double) dailyPEF / (double) pb;
        if (percentage >= 0.8) return "GREEN";
        else if (percentage >= 0.5) return "YELLOW";
        else return "RED";
    }

    private void logZoneChange(String childUid,
                               String childName,
                               String oldZone,
                               String newZone,
                               long dailyPEF,
                               long pb) {

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        double percentage = pb > 0 ? (double) dailyPEF / (double) pb : 0.0;

        Map<String, Object> zoneData = new HashMap<>();
        zoneData.put("childId", childUid);
        zoneData.put("childName", childName);
        zoneData.put("date", date);
        zoneData.put("oldZone", oldZone);
        zoneData.put("newZone", newZone);
        zoneData.put("dailyPEF", dailyPEF);
        zoneData.put("pb", pb);
        zoneData.put("percentage", percentage);
        zoneData.put("timestamp", System.currentTimeMillis());

        // Parent-level zone history collection
        db.collection("users")
                .document(parentID)
                .collection("zoneHistory")
                .add(zoneData);
    }

    public void savePEF() {
        Object tag = chooseChildButton.getTag();

        if (tag == null) {
            Toast.makeText(this, "Please choose a child", Toast.LENGTH_SHORT).show();
            return;
        }

        String childUid = tag.toString();
        String childName = chooseChildButton.getText().toString();

        int dailyPEF = pefParser.parsePEF(dailyPEFInput);
        int prePEF = pefParser.parsePEF(preMedicationPB);
        int postPEF = pefParser.parsePEF(postMedicationPB);

        if (dailyPEF == 0) {
            Toast.makeText(this, "Please enter the daily PEF", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageChild entry = new StorageChild(dailyPEF, prePEF, postPEF);
        pefStorage.save(childUid, entry);

        saveChildPEFToFirebase(childUid, childName, entry);
    }

    private void saveChildPEFToFirebase(String childUid, String childName, StorageChild entry) {

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

                    Long pbValue = latestDoc.getLong("pb");
                    long pb = pbValue != null ? pbValue : 0;

                    String oldZone = computeZone(oldDaily, pb);

                    long finalDaily = Math.max(oldDaily, entry.getDailyPEF());
                    long finalPre = Math.max(oldPre, entry.getPrePEF());
                    long finalPost = Math.max(oldPost, entry.getPostPEF());

                    String newZone = computeZone(finalDaily, pb);

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
                            // IMPORTANT: merge so pb/timestamp are preserved
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "PEF saved successfully", Toast.LENGTH_SHORT).show();

                                // Only log when there *is* a change in zone
                                if (oldZone != null && newZone != null && !oldZone.equals(newZone)) {
                                    logZoneChange(childUid, childName, oldZone, newZone, finalDaily, pb);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Log.e("PEFActivity", "Error saving PEF", e));
                });
    }
}