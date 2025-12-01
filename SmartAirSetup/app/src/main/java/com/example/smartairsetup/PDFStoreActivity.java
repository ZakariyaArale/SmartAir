package com.example.smartairsetup;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class PDFStoreActivity extends AppCompatActivity {

    private Button chooseChildButton;
    private Button chooseDateButton;
    private Button downloadPdfButton;
    private Button backButton;

    private long chosenTimestamp = -1;
    private PDFGenerator pdfGenerator;

    private String parentID;
    private String childID;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        chooseChildButton = findViewById(R.id.chooseChildButton);
        chooseDateButton = findViewById(R.id.chooseDateButton);
        downloadPdfButton = findViewById(R.id.btnDownloadPdf);
        backButton = findViewById(R.id.backButton);


        pdfGenerator = new PDFGenerator(this);

        // Get IDs passed via Intent
        Intent intent = getIntent();
        parentID = intent.getStringExtra("PARENT_UID");
        childID = intent.getStringExtra("CHILD_ID");
        childName = intent.getStringExtra("CHILD_NAME");

        if (childID == null || childID.isEmpty()) {
            Toast.makeText(this, "Child ID not provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Show selected child
        chooseChildButton.setText("Child: " + (childName != null ? childName : childID));
        chooseChildButton.setEnabled(false); // disable selection if only one child

        // Date selection
        chooseDateButton.setOnClickListener(v -> openRestrictedDatePicker());

        // Download PDF
        downloadPdfButton.setOnClickListener(v -> {
            if (chosenTimestamp == -1) {
                Toast.makeText(this, "Please choose a date first", Toast.LENGTH_SHORT).show();
                return;
            }

            long startTimestamp = chosenTimestamp;
            long endTimestamp = Calendar.getInstance().getTimeInMillis();

            Log.d("PDFStoreActivity", "parentID=" + parentID + ", childID=" + childID);
            pdfGenerator.generatePdf(parentID, childID, startTimestamp, endTimestamp);
        });

        // Back button
        backButton.setOnClickListener(v -> finish());
    }

    private void openRestrictedDatePicker() {
        // Today (real current date)
        Calendar today = Calendar.getInstance();

        // Earliest date user can choose (6 months ago)
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.MONTH, -6);

        // Latest date that satisfies the "3–6 months from today" rule
        Calendar latestAllowed = Calendar.getInstance();
        latestAllowed.add(Calendar.MONTH, -3);

        // Initial date the picker shows:
        //  - if user already chose a date, reuse it
        //  - otherwise, show *today* as the current date
        Calendar initial = Calendar.getInstance();
        if (chosenTimestamp != -1) {
            initial.setTimeInMillis(chosenTimestamp);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(y, m, d, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);

                    // Still enforce "3–6 months from today"
                    if (selected.before(minDate) || selected.after(latestAllowed)) {
                        Toast.makeText(this, "Date must be 3–6 months from today.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    chosenTimestamp = selected.getTimeInMillis();
                    chooseDateButton.setText((m + 1) + "/" + d + "/" + y);
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );

        // Hard bounds on the widget: from 6 months ago up to *today* (no future dates)
        dialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        dialog.getDatePicker().setMaxDate(today.getTimeInMillis());
        dialog.show();
    }
}
