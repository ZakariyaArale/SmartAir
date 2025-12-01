package com.example.smartairsetup.triage;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.widget.Button;

import com.example.smartairsetup.pef.ChildFetchListener;
import com.example.smartairsetup.pef.ProcessChildren;
import com.example.smartairsetup.pef.UserID;

import java.util.List;

public class MedicationDialog {

    private static final String TAG = "MedicationDialog";

    private final Activity activity;
    private final ProcessChildren provider;

    public MedicationDialog(Activity activity, ProcessChildren provider) {
        this.activity = activity;
        this.provider = provider;
    }

    public void showSelectionDialog(Button chooseMedButton) {
        Log.d(TAG, "showSelectionDialog called");

        provider.getChildren(new ChildFetchListener() {
            @Override
            public void onChildrenLoaded(List<UserID> medsList) {
                Log.d(TAG, "onChildrenLoaded called, medsList size: " + medsList.size());

                if (medsList.isEmpty()) {
                    Log.d(TAG, "No medications found for child");
                    new AlertDialog.Builder(activity)
                            .setTitle("Select Medication")
                            .setItems(new String[]{"No medications found"}, (d, w) -> {})
                            .show();
                    return;
                }

                CharSequence[] names = new CharSequence[medsList.size()];
                for (int i = 0; i < medsList.size(); i++) {
                    names[i] = medsList.get(i).name;
                    Log.d(TAG, "Medication loaded: " + medsList.get(i).name + " (UID: " + medsList.get(i).uid + ")");
                }

                new AlertDialog.Builder(activity)
                        .setTitle("Select Medication")
                        .setItems(names, (dialog, which) -> {
                            UserID selectedMed = medsList.get(which);
                            Log.d(TAG, "Selected medication: " + selectedMed.name + " (UID: " + selectedMed.uid + ")");
                            chooseMedButton.setText(selectedMed.name);
                            chooseMedButton.setTag(selectedMed.uid);
                        })
                        .show();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load medications", e);
                chooseMedButton.setText("Failed to load medications");
            }
        });
    }
}
