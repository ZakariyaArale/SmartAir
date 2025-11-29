package com.example.smartairsetup;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


public class MedicationInventoryActivity extends AppCompatActivity {
    private ScrollView medDetailsSV;
    private TextView medNameTV;
    private TextView purchaseDateTV;
    private TextView expiryDateTV;
    private TextView doseLeftTV;
    private TextView medNotesTV;
    private Button backButton;

    private Button newMedButton;
    private Button deleteMedButton;
    private Button editMedButton;
    private Button notifButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String parentUID;
    private List<ChildMedicationWrapper> allMeds = new ArrayList<>();
    private ChildMedicationWrapper selectedMed = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medication_inventory);

        medDetailsSV = findViewById(R.id.medDetailsSV);
        medNameTV = findViewById(R.id.medNameTV);
        purchaseDateTV = findViewById(R.id.purchaseDateTV);
        expiryDateTV = findViewById(R.id.expiryDateTV);
        doseLeftTV = findViewById(R.id.dosesLeftTV);
        medNotesTV = findViewById(R.id.medNotesTV);

        setBackButton();
        setNewMedButton();
        setDeleteButton();
        setEditButton();
        setNotifButton();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadAllMedications();  //refreshes medications to reflect changes
        medDetailsSV.setVisibility(View.INVISIBLE); //hides last clicked med info

        deleteMedButton.setEnabled(false);
        deleteMedButton.setAlpha(0.5f); //greys out buttons
        editMedButton.setEnabled(false);
        editMedButton.setAlpha(0.5f);

    }


    private void loadAllMedications() {

        allMeds.clear();

        // Auth guard - checks if parent is logged in
        if (mAuth.getCurrentUser() != null) {
            parentUID = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this,
                    "You must be logged in as a parent.",
                    Toast.LENGTH_LONG).show();
            finish();
        }


        //initialize the recycle viewer list and adapter that adds meds to list
        RecyclerView recyclerView = findViewById(R.id.medicineList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        MedicationAdapter  adapter = new MedicationAdapter(allMeds,
                this::showDetails);
        recyclerView.setAdapter(adapter);

        //find all children associated with user
        db.collection("users")
                .document(parentUID)
                .collection("children")
                .get()
                .addOnSuccessListener(childrenSnap -> {

                    if (childrenSnap.isEmpty()) {
                        return;
                    }

                    //get medications for each child
                    for (DocumentSnapshot childDoc : childrenSnap) {
                        String childUID = childDoc.getId();
                        String childName = childDoc.getString("name");

                        db.collection("users")
                                .document(parentUID)
                                .collection("children")
                                .document(childUID)
                                .collection("medications")
                                .get()
                                .addOnSuccessListener(medSnap -> {

                                        for (DocumentSnapshot medDoc : medSnap) {
                                            Medication med = medDoc.toObject(Medication.class);
                                            allMeds.add(new ChildMedicationWrapper(childName, childUID, med));
                                        }
                                        adapter.notifyDataSetChanged();  // <--- THIS is what makes them appear

                                })
                                .addOnFailureListener(e -> {
                                    Log.e("LOAD_MEDS", "Failed to load meds for " + childUID, e);
                                });
                    }


                })
                .addOnFailureListener(e -> {
                    Log.e("LOAD_MEDS", "Failed to load children", e);
                });
    }

    public void showDetails(ChildMedicationWrapper medList) {
        selectedMed = medList; // saving the med allows for editing/deleting

        // Make the container visible
        medDetailsSV.setVisibility(View.VISIBLE);

        deleteMedButton.setEnabled(true);
        deleteMedButton.setAlpha(1f); //greys out buttons
        editMedButton.setEnabled(true);
        editMedButton.setAlpha(1f);

        String rescueText;
        // Fill in the details
        if (medList.getMed().getisRescue()){
            rescueText = "  -  Rescue";
        }else{
            rescueText = "";
        }

        medNameTV.setText(medList.getMed().getName() + "  -  " + medList.getChildName()
                + rescueText);
        purchaseDateTV.setText("Purchased: "
                + medList.getMed().getPurchaseDay() + "/"
                + medList.getMed().getPurchaseMonth() + "/"
                + medList.getMed().getPurchaseYear());
        expiryDateTV.setText("Expires: "
                + medList.getMed().getExpiryDay() + "/"
                + medList.getMed().getExpiryMonth() + "/"
                + medList.getMed().getExpiryYear());
        doseLeftTV.setText("Doses left: " +medList.getMed().getPuffsLeft());
        medNotesTV.setText("Additional Notes: " + medList.getMed().getNotes());

    }

    private void setBackButton() {
        backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, ParentHomeActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setNewMedButton() {
        newMedButton = findViewById(R.id.newMedButton);
        if (newMedButton != null) {
            newMedButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddEditMedicationActivity.class);
                intent.putExtra("mode", "new");
                startActivity(intent);
            });
        }
    }

    private void setDeleteButton(){

        deleteMedButton = findViewById(R.id.deleteMedButton);
        if (deleteMedButton != null) {
            deleteMedButton.setOnClickListener(v -> {

                new AlertDialog.Builder(this)
                        .setTitle("Are you sure?") //set's title of pop-up
                        .setMessage("Once your medication is deleted it will be erased permanently," +
                                " you cannot recover it.")
                        .setPositiveButton("Yes! Delete it.", (dialog, which) -> {

                            //safety check, shouldn't occur if buttons are enabled/disable properly
                            if (selectedMed == null) {
                                Toast.makeText(this, "No medication selected.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            db.collection("users")
                                    .document(parentUID)
                                    .collection("children")
                                    .document(selectedMed.getChildID())
                                    .collection("medications")
                                    .document(selectedMed.getMed().getMed_UUID())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {

                                        Toast.makeText(this, "Medication deleted!",
                                                Toast.LENGTH_SHORT).show();

                                        allMeds.remove(selectedMed);
                                        selectedMed = null;
                                        medDetailsSV.setVisibility(View.INVISIBLE);

                                        deleteMedButton.setEnabled(false);
                                        deleteMedButton.setAlpha(0.5f); //greys out buttons
                                        editMedButton.setEnabled(false);
                                        editMedButton.setAlpha(0.5f);

                                        // refresh list
                                        loadAllMedications();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to delete: "
                                                + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });

                        })
                        //allows user to leave notification set up till later
                        .setNegativeButton("Maybe Later", (dialog,
                                                           which) -> {
                            dialog.dismiss();
                        })
                        .setCancelable(false) //doesn't allow user to click out of the pop-up
                        // by clicking elsewhere
                        .show(); //makes constructed pop-up visible

            });
        }


    }

    private void setEditButton(){

        editMedButton = findViewById(R.id.editMedButton);
        if (editMedButton != null) {
            editMedButton.setOnClickListener(v -> {

                if (selectedMed == null) {
                    Toast.makeText(this, "No medication selected.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(this, AddEditMedicationActivity.class);
                intent.putExtra("mode", "edit");
                intent.putExtra("passedChildUID", selectedMed.getChildID());
                intent.putExtra("passedMedID", selectedMed.getMed().getMed_UUID());
                startActivity(intent);


            });
        }
    }

    private void setNotifButton(){
        notifButton = findViewById(R.id.buttonNotif);
        notifButton.setOnClickListener(this::onNotifClick);

    }

    private void onNotifClick(View v) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        //Sets up permissions for sending notifications/alarms.
        if (!NotificationPermissionsHelper.ensureNotificationPermissions(this)
                || !NotificationPermissionsHelper.ensureAlarmPermissions(this) ) return;


        long triggerTime = System.currentTimeMillis() + 60 * 1000; // 1 minute later

        //error is due to API difference. Have to revamp notifications to work on older API tk
        if(alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent); //tk
        }
        Toast.makeText(this, "Notification scheduled for 1 minute from now!", Toast.LENGTH_SHORT).show();
    }
}