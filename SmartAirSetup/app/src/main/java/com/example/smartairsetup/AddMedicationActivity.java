package com.example.smartairsetup;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Medication will be stored under:
// users/{parentUid}/children/{childId}/medications/{med_UUID}
public class AddMedicationActivity extends AppCompatActivity {

    public static final String EXTRA_CHILD_ID = "childId";

    private EditText editMedName;
    private EditText editPurchaseDay;
    private EditText editPurchaseMonth;
    private EditText editPurchaseYear;
    private EditText editExpiryDay;
    private EditText editExpiryMonth;
    private EditText editExpiryYear;
    private EditText editPuffsLeft;
    private EditText editReminderDays;
    private EditText editThreshold;

    private TextView textMedicationError;
    private Button buttonSaveMedication;
    private Button buttonBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String parentUid;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medication);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        buttonBack = findViewById(R.id.buttonBack);
        editMedName = findViewById(R.id.editMedName);
        editPurchaseDay = findViewById(R.id.editPurchaseDay);
        editPurchaseMonth = findViewById(R.id.editPurchaseMonth);
        editPurchaseYear = findViewById(R.id.editPurchaseYear);
        editExpiryDay = findViewById(R.id.editExpiryDay);
        editExpiryMonth = findViewById(R.id.editExpiryMonth);
        editExpiryYear = findViewById(R.id.editExpiryYear);
        editPuffsLeft = findViewById(R.id.editPuffsLeft);
        editReminderDays = findViewById(R.id.editReminderDays);
        editThreshold = findViewById(R.id.editThreshold);
        textMedicationError = findViewById(R.id.textMedicationError);
        buttonSaveMedication = findViewById(R.id.buttonSaveMedication);

        // Auth guard
        if (mAuth.getCurrentUser() != null) {
            parentUid = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this,
                    "You must be logged in as a parent.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Child id from intent
        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        if (childId == null || childId.trim().isEmpty()) {
            Toast.makeText(this,
                    "No child selected.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Listeners
        buttonBack.setOnClickListener(v -> onBackPressed());
        buttonSaveMedication.setOnClickListener(v -> saveMedication());
    }

    private void saveMedication() {
        textMedicationError.setVisibility(View.GONE);
        textMedicationError.setText("");

        String name = editMedName.getText().toString().trim();

        String purchaseDayStr = editPurchaseDay.getText().toString().trim();
        String purchaseMonthStr = editPurchaseMonth.getText().toString().trim();
        String purchaseYearStr = editPurchaseYear.getText().toString().trim();

        String expiryDayStr = editExpiryDay.getText().toString().trim();
        String expiryMonthStr = editExpiryMonth.getText().toString().trim();
        String expiryYearStr = editExpiryYear.getText().toString().trim();

        String puffsLeftStr = editPuffsLeft.getText().toString().trim();
        String reminderDaysStr = editReminderDays.getText().toString().trim();
        String thresholdStr = editThreshold.getText().toString().trim();

        // ===== Required fields =====
        if (TextUtils.isEmpty(name)) {
            editMedName.setError("Medication name is required");
            editMedName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(purchaseDayStr) ||
                TextUtils.isEmpty(purchaseMonthStr) ||
                TextUtils.isEmpty(purchaseYearStr)) {
            textMedicationError.setText("Purchase date is required");
            textMedicationError.setVisibility(View.VISIBLE);
            return;
        }

        if (TextUtils.isEmpty(expiryDayStr) ||
                TextUtils.isEmpty(expiryMonthStr) ||
                TextUtils.isEmpty(expiryYearStr)) {
            textMedicationError.setText("Expiry date is required");
            textMedicationError.setVisibility(View.VISIBLE);
            return;
        }

        if (TextUtils.isEmpty(puffsLeftStr)) {
            editPuffsLeft.setError("Puffs left is required");
            editPuffsLeft.requestFocus();
            return;
        }

        int purchaseDay, purchaseMonth, purchaseYear;
        int expiryDay, expiryMonth, expiryYear;
        int puffsLeft;
        int puffNearEmptyThreshold = 0; // default if empty

        try {
            purchaseDay = Integer.parseInt(purchaseDayStr);
            purchaseMonth = Integer.parseInt(purchaseMonthStr);
            purchaseYear = Integer.parseInt(purchaseYearStr);

            expiryDay = Integer.parseInt(expiryDayStr);
            expiryMonth = Integer.parseInt(expiryMonthStr);
            expiryYear = Integer.parseInt(expiryYearStr);

            puffsLeft = Integer.parseInt(puffsLeftStr);

            if (!TextUtils.isEmpty(thresholdStr)) {
                puffNearEmptyThreshold = Integer.parseInt(thresholdStr);
            }
        } catch (NumberFormatException e) {
            textMedicationError.setText("Please enter valid numbers for dates, puffs, and threshold.");
            textMedicationError.setVisibility(View.VISIBLE);
            return;
        }

        // Parse reminderDays: "30,7,1" -> [30,7,1]
        List<Integer> reminderDaysList = new ArrayList<>();
        if (!TextUtils.isEmpty(reminderDaysStr)) {
            String[] parts = reminderDaysStr.split(",");
            try {
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        reminderDaysList.add(Integer.parseInt(trimmed));
                    }
                }
            } catch (NumberFormatException e) {
                textMedicationError.setText("Reminder days must be a comma-separated list of numbers.");
                textMedicationError.setVisibility(View.VISIBLE);
                return;
            }
        }

        buttonSaveMedication.setEnabled(false);

        // Collection: users/{parentUid}/children/{childId}/medications
        CollectionReference medsRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medications");

        // Pre-generate doc ID so we can store med_UUID
        DocumentReference medDoc = medsRef.document();
        String medUuid = medDoc.getId();

        Map<String, Object> medData = new HashMap<>();
        medData.put("med_UUID", medUuid);
        medData.put("name", name);

        medData.put("purchaseDay", purchaseDay);
        medData.put("purchaseMonth", purchaseMonth);
        medData.put("purchaseYear", purchaseYear);

        medData.put("expiryDay", expiryDay);
        medData.put("expiryMonth", expiryMonth);
        medData.put("expiryYear", expiryYear);

        medData.put("puffsLeft", puffsLeft);
        medData.put("puffNearEmptyThreshold", puffNearEmptyThreshold);
        medData.put("reminderDays", reminderDaysList); // Firestore stores as array

        medData.put("active", true);
        medData.put("createdAt", new Date());

        medDoc.set(medData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(AddMedicationActivity.this,
                            "Medication added",
                            Toast.LENGTH_SHORT).show();
                    buttonSaveMedication.setEnabled(true);
                    finish();
                })
                .addOnFailureListener(e -> {
                    textMedicationError.setText("Failed to add medication: " + e.getMessage());
                    textMedicationError.setVisibility(View.VISIBLE);
                    buttonSaveMedication.setEnabled(true);
                });
    }
}