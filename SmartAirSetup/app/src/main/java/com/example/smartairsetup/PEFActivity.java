package com.example.smartairsetup;

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
import java.util.HashMap;
import java.util.Locale;
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

        // Get current logged-in user's UID
        parentID = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (parentID == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Child selection dialog
        ProcessChildren provider = new FireBaseProcessChild();
        ChildDiaglog childDiaglog = new ChildDiaglog(this, provider);
        chooseChildButton.setOnClickListener(v -> childDiaglog.showSelectionDialog(chooseChildButton));
        saveButton.setOnClickListener(v -> savePEF());

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private String computeZone(long dailyPEF, long pb) {
        if (dailyPEF <= 0 || pb <= 0) return null;
        double percentage = (double) dailyPEF / pb;
        if (percentage >= 0.8) return "GREEN";
        else if (percentage >= 0.5) return "YELLOW";
        else return "RED";
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

        // Use today's date dynamically
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        var childRef = db.collection("users").document(parentID).collection("children").document(childUid);
        var logsCollection = childRef.collection("PEF").document("logs").collection("daily");
        var latestRef = childRef.collection("PEF").document("latest");

        // Get PB from child document
        childRef.get().addOnSuccessListener(childDoc -> {
            Long pbValue = childDoc.getLong("pb");
            long pb = pbValue != null ? pbValue : 0;

            if (pb == 0) {
                Toast.makeText(this, "PB not set for child", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if log for today exists
            var todayLogRef = logsCollection.document(todayDate);
            todayLogRef.get().addOnSuccessListener(todayDoc -> {
                long oldDaily = todayDoc.getLong("dailyPEF") != null ? todayDoc.getLong("dailyPEF") : 0;

                // Only overwrite if new dailyPEF is greater
                if (entry.getDailyPEF() >= oldDaily) {
                    String zone = computeZone(entry.getDailyPEF(), pb);

                    Map<String, Object> logData = new HashMap<>();
                    logData.put("date", todayDate);
                    logData.put("dailyPEF", entry.getDailyPEF());
                    logData.put("prePEF", entry.getPrePEF());
                    logData.put("postPEF", entry.getPostPEF());
                    logData.put("pb", pb);
                    logData.put("zone", zone);
                    logData.put("timestamp", System.currentTimeMillis());

                    todayLogRef.set(logData, SetOptions.merge())
                            .addOnSuccessListener(a -> Toast.makeText(this, "PEF log updated", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Log.e("PEFActivity", "Error updating log", e));

                    // Update latest
                    Map<String, Object> latestData = new HashMap<>();
                    latestData.put("dailyPEF", entry.getDailyPEF());
                    latestData.put("prePEF", entry.getPrePEF());
                    latestData.put("postPEF", entry.getPostPEF());
                    latestData.put("pb", pb);
                    latestData.put("zone", zone);
                    latestData.put("timestamp", System.currentTimeMillis());

                    latestRef.set(latestData, SetOptions.merge());
                }
            });
        });
    }
}
