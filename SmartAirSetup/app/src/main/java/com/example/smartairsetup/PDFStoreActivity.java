package com.example.smartairsetup;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class PDFStoreActivity extends AppCompatActivity {

    private Button chooseChildButton;
    private Button chooseDateButton;
    private Button downloadPdfButton;

    private long chosenTimestamp = -1;
    private PDFGenerator pdfGenerator;

    // Hardcoded parent and child for testing
    private final String parentID = "VfB95gwXXyWFAqdajTHJBgyeYfB3";
    private final String childID = "gifrbhr98mAAyv78MC80";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        // chooseChildButton = findViewById(R.id.chooseChildButton);
        // chooseDateButton = findViewById(R.id.chooseDateButton);
        // downloadPdfButton = findViewById(R.id.btnDownloadPdf);

        pdfGenerator = new PDFGenerator(this);

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

            pdfGenerator.generatePdf(parentID, childID, startTimestamp, endTimestamp);
        });

        // Optional: Disable child selection button (we're hardcoding)
        chooseChildButton.setText("Child UID: " + childID);
    }

    private void openRestrictedDatePicker() {
        Calendar today = Calendar.getInstance();

        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.MONTH, -6);

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, -3);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(y, m, d);

                    if (selected.before(minDate) || selected.after(maxDate)) {
                        Toast.makeText(this, "Date must be 3–6 months from today.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    chosenTimestamp = selected.getTimeInMillis();
                    chooseDateButton.setText((m + 1) + "/" + d + "/" + y);
                },
                maxDate.get(Calendar.YEAR),
                maxDate.get(Calendar.MONTH),
                maxDate.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        dialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        dialog.show();
    }
}
