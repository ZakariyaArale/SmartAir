package com.example.smartairsetup;

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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MedicationInventoryActivity extends AppCompatActivity {

    //TEMPORARY HARD CODED FIELD: tk
    List<Medication> hardCodedMedList;

    private LinearLayout medDetailsContainerLL;
    private TextView medNameTV;
    private TextView purchaseDateTV;
    private TextView expiryDateTV;
    private TextView doseLeftTV;
    private TextView remindersTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medication_inventory);


        // tk I added what's below.

        hardCodedMedList = new ArrayList<>(); ///////////////////////////////////////////////This is a hard coded list, this should be replaced by Firebase stuff
        hardCodedMedList.add(new Medication("UUIDExample", "example name",
                13, 7, 2014, 1, 8,
                2023, new int [] {1,2, 3}, 50, 7));
        hardCodedMedList.add(new Medication("UUIDExample2", "example name2",
                14, 4, 2018, 1, 8,
                2023, new int [] {1,2,     7}, 56, 9));
        hardCodedMedList.add(new Medication("UUIDExample2", "example name2",
                14, 4, 2018, 1, 8,
                2023, new int [] {1,2,     7}, 56, 9));
        hardCodedMedList.add(new Medication("UUIDExample2", "example name2",
                14, 4, 2018, 1, 8,
                2023, new int [] {1,2,     7}, 56, 9));
        hardCodedMedList.add(new Medication("UUIDExample2", "example name2",
                14, 4, 2018, 1, 8,
                2023, new int [] {1,2,     7}, 56, 9));
        hardCodedMedList.add(new Medication("UUIDExample2", "example name2",
                14, 4, 2018, 1, 8,
                2023, new int [] {1,2,     7}, 56, 9));

        medDetailsContainerLL = findViewById(R.id.medDetailsContainer);
        medNameTV = findViewById(R.id.tvMedName);
        purchaseDateTV = findViewById(R.id.tvPurchaseDate);
        expiryDateTV = findViewById(R.id.tvExpiryDate);
        doseLeftTV = findViewById(R.id.tvPuffsLeft);
        remindersTV = findViewById(R.id.tvReminders);

        setBackButton();
        setNewMedButton();


        Button myButton = findViewById(R.id.buttonNotif);

        myButton.setOnClickListener(this::onClick);

        RecyclerView recyclerView = findViewById(R.id.medicineList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        MedicationAdapter adapter = new MedicationAdapter(
                hardCodedMedList, //yourMedicationList, //replace with list of medication // tk
                med -> {
                    showDetails(med); // function that fills the lower list //tk

                    //enable buttons to edit/delete


                }
        );

        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    public void showDetails(Medication med) {
        // Make the container visible
        medDetailsContainerLL.setVisibility(View.VISIBLE);

        // Fill in the details
        medNameTV.setText(med.getName());
        purchaseDateTV.setText("Purchased: "
                + med.getPurchaseDay() + "/"
                + med.getPurchaseMonth() + "/"
                + med.getPurchaseYear());
        expiryDateTV.setText("Expires: "
                + med.getExpiryDay() + "/"
                + med.getExpiryMonth() + "/"
                + med.getExpiryYear());
        doseLeftTV.setText("Doses left: " + med.getPuffsLeft());
        remindersTV.setText("Remind " + Arrays.toString(med.getReminderDays()) + " days ahead");

    }

    private void setBackButton() {
        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setNewMedButton() {
        Button newMedButton = findViewById(R.id.newMedButton);
        if (newMedButton != null) {
            newMedButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddMedicationActivityV2.class);
                startActivity(intent);
            });
        }
    }

    private void onClick(View v) {
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

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent); //tk

        Toast.makeText(this, "Notification scheduled for 1 minute from now!", Toast.LENGTH_SHORT).show();
    }
}