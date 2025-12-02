package com.example.smartairsetup.medlog;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartairsetup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddEditMedicationActivity extends AppCompatActivity {

    private EditText medNameET;
    private EditText purchaseDateET;
    private EditText expiryDateET;
    private NumberPicker doseCountNP;
    private EditText notesET;
    private Spinner childSpinner;
    private Switch rescueMedSwitch;

    private FirebaseFirestore db;

    private String parentUid;
    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();

    private boolean isEditMode = false;
    private String passedMedID;
    private String passedChildUID;
    private Medication editingMedication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_medications);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        getIds();
        setCancelButton();

        setDosePicker();
        setUpDate(R.id.purchaseDateET);
        setUpDate(R.id.expiryDateET);

        // Auth guard - checks if parent is logged in
        if (mAuth.getCurrentUser() != null) {
            parentUid = mAuth.getCurrentUser().getUid();
            Log.d("DEBUG_UID", "Using parent UID = " + parentUid);
        } else {
            Toast.makeText(this,
                    "You must be logged in as a parent.",
                    Toast.LENGTH_LONG).show();
            finish();
        }

        loadChildren();

        //checks mode, if in edit mode, fill in values and set up so it will edit medication
        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");

        if (mode == null || "edit".equals(mode)) {
            isEditMode = true;
            passedChildUID= intent.getStringExtra("passedChildUID");
            passedMedID = intent.getStringExtra("passedMedID");
            getMedValues();

            //don't allow for editing child as it causes weird things with medications logs,
            // below code is still set up to allow for changing children to make this
            // easier to implement if our app is chosen
            childSpinner.setEnabled(false);
            childSpinner.setAlpha(0.5f);

        } else {
            isEditMode = false;
        }

        //this needs to be after as isEditMode is used
        setSaveButton();

    }

    private void getMedValues() {

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(passedChildUID)
                .collection("medications")
                .document(passedMedID)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        editingMedication = doc.toObject(Medication.class);
                        if (editingMedication != null) {
                            fillFieldsForEdit(editingMedication);
                        }
                    }
                });

    }

    private void fillFieldsForEdit(Medication med) {

        medNameET.setText(med.getName());
        notesET.setText(med.getNotes());
        purchaseDateET.setText(med.getPurchaseYear() + "-" +(med.getPurchaseMonth())
                + "-" +(med.getPurchaseDay()));
        expiryDateET.setText(med.getExpiryYear() + "-" +(med.getExpiryMonth())
                + "-" +(med.getExpiryDay()));
        doseCountNP.setValue(med.getPuffsLeft());
        rescueMedSwitch.setChecked(med.getisRescue());


    }

    private void getIds() {
        medNameET = findViewById(R.id.medNameET);
        purchaseDateET = findViewById(R.id.purchaseDateET);
        expiryDateET = findViewById(R.id.expiryDateET);
        doseCountNP = findViewById(R.id.doseCountNP);
        notesET = findViewById(R.id.notesET);
        rescueMedSwitch = findViewById(R.id.rescueMedSwitch);

    }

    private void setCancelButton() {
        Button cancelButton = findViewById(R.id.cancelButton);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> finish());
        }
    }

    private void setSaveButton() {

        Button saveButton = findViewById(R.id.saveButton);
        if (saveButton != null) {

            //set's text to reflect if user is editing or adding a med
            if (isEditMode) {
                saveButton.setText("Save Changes");
            } else {
                saveButton.setText("Add Medication");
            }

            saveButton.setOnClickListener(v -> {

                //gets values from form
                int position = childSpinner.getSelectedItemPosition();
                String selectedChildId = childIds.get(position);
                String medName = medNameET.getText().toString().trim();
                String purchase = purchaseDateET.getText().toString().trim();
                String expiry = expiryDateET.getText().toString().trim();
                String notes = notesET.getText().toString().trim(); // may be empty
                boolean rescue = rescueMedSwitch.isChecked();
                int DosesLeft = doseCountNP.getValue();
                //[0] = year, [1] = month, [2] = day
                int[] purchaseDate = getDateInts(purchaseDateET);
                int[] expiryDate = getDateInts(expiryDateET);
                int puffNearEmptyThreshold = 0; // default as currently no way to input tk

                if (!validateForm(medName, purchase, expiry))
                    return;

                //only get here if form is valid inputs
                saveButton.setEnabled(false);


                //get data ready for storage
                Map<String, Object> medData = new HashMap<>();
                medData.put("name", medName);

                medData.put("isRescue", rescue);

                medData.put("purchaseDay", purchaseDate[2]);
                medData.put("purchaseMonth", purchaseDate[1]);
                medData.put("purchaseYear", purchaseDate[0]);

                medData.put("expiryDay", expiryDate[2]);
                medData.put("expiryMonth", expiryDate[1]);
                medData.put("expiryYear", expiryDate[0]);

                medData.put("puffsLeft", DosesLeft);
                medData.put("puffNearEmptyThreshold", puffNearEmptyThreshold);

                medData.put("notes", notes);

                medData.put("reminderDays", new ArrayList<Integer>());

                medData.put("active", true);

                if (isEditMode) {

                    medData.put("med_UUID", passedMedID);      // keep same UUID
                    medData.put("createdAt", editingMedication.getCreatedAt()); // keep same timestamp

                    if (!selectedChildId.equals(passedChildUID)) {
                        //Delete old medication
                        db.collection("users")
                                .document(parentUid)
                                .collection("children")
                                .document(passedChildUID) // OLD child
                                .collection("medications")
                                .document(passedMedID)
                                .delete()
                                .addOnSuccessListener(unused -> {

                                    //add medication to selected child
                                    db.collection("users")
                                            .document(parentUid)
                                            .collection("children")
                                            .document(selectedChildId) // NEW child
                                            .collection("medications")
                                            .document(passedMedID)
                                            .set(medData)
                                            .addOnSuccessListener(v2 -> {
                                                Toast.makeText(this, "Medication moved & updated!", Toast.LENGTH_SHORT).show();
                                                saveButton.setEnabled(true);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Failed writing to new child", Toast.LENGTH_LONG).show();
                                                saveButton.setEnabled(true);
                                            });

                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed removing from old child", Toast.LENGTH_LONG).show();
                                    saveButton.setEnabled(true);
                                });

                    } else {
                        // Normal update (child not changed)
                        db.collection("users")
                                .document(parentUid)
                                .collection("children")
                                .document(selectedChildId)
                                .collection("medications")
                                .document(passedMedID)
                                .update(medData)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Medication updated!", Toast.LENGTH_SHORT).show();
                                    saveButton.setEnabled(true);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "FAILED TO UPDATE MEDICATION", Toast.LENGTH_LONG).show();
                                    saveButton.setEnabled(true);
                                });
                        }

                } else {

                    DocumentReference newDoc = db.collection("users")
                            .document(parentUid)
                            .collection("children")
                            .document(selectedChildId)
                            .collection("medications")
                            .document();

                    medData.put("med_UUID", newDoc.getId());
                    medData.put("createdAt", new Date());

                    newDoc.set(medData)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Medication added!", Toast.LENGTH_SHORT).show();
                                saveButton.setEnabled(true);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "FAILED TO ADD MEDICATION", Toast.LENGTH_LONG).show();
                                saveButton.setEnabled(true);
                            });
                }
            });
        }
    }

    private void setDosePicker() {

        NumberPicker picker = findViewById(R.id.doseCountNP);
        picker.setMinValue(0);
        //600 is a reasonable bound but want to be safe in case someone has a weird inhaler
        picker.setMaxValue(900);
        picker.setValue(200);
        picker.setWrapSelectorWheel(false);

    }

    private void setUpDate(int idOfTV) {

        EditText DateET = findViewById(idOfTV);

        //makes a pop up dialog so user can select date of purchase
        DateET.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, y, m, d) ->
                    {
                        DateET.setText(y + "-" + (m + 1) + "-" + d);
                        DateET.setError(null); //manually gets rid of error message as user
                        // technically isn't editing the editText
                    },
                    year, month, day
            );

            dialog.show();
        });

    }

    private boolean validateForm(String medName, String purchase, String expiry) {

        boolean flag = true;

        // checks Medication name is filled in
        if (medName.isEmpty()) {
            medNameET.setError("Medication name is required");
            medNameET.requestFocus();
            flag = false;
        }


        // checks purchase date is selected, note the date fields are unfocusable
        // so request focus isn't run
        if (purchase.isEmpty()) {
            purchaseDateET.setError("Please select a purchase date");
            flag = false;
        }else if(getDateInts(purchaseDateET)[0] == -1){
            purchaseDateET.setError("Purchase Date Format is Invalid");
            flag = false;
        }

        // checks expiry date is selected
        if (expiry.isEmpty()) {
            expiryDateET.setError("Please select an expiry date");
            flag = false;
        }else if(getDateInts(expiryDateET)[0] == -1){
            expiryDateET.setError("Expiry Date Format is Invalid");
            flag = false;
        }

        return flag;
    }

    private int[] getDateInts(EditText et) {
        try {
            String[] parts = et.getText().toString().split("-");

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            return new int[]{year, month, day};
        }catch (Exception e){
            return new int[]{-1, -1, -1}; //returns -1 if date sting format invalid
        }
    }

    private void loadChildren() {

        childSpinner = findViewById(R.id.childSpinner);

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String name = doc.getString("name");
                        childNames.add(name);
                        childIds.add(doc.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            childNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    childSpinner.setAdapter(adapter);

                    //sets it to passed ID if in edit mode
                    if (passedChildUID != null) {
                        int index = childIds.indexOf(passedChildUID);
                        if (index != -1) {
                            childSpinner.setSelection(index);
                        }
                    }
                });
    }

}

