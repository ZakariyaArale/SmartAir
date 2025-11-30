package com.example.smartairsetup;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Button;

import java.util.List;

public class MedicationDialog {

    private final Activity activity;
    private final ProcessChildren provider;

    public MedicationDialog(Activity activity, ProcessChildren provider) {
        this.activity = activity;
        this.provider = provider;
    }

    public void showSelectionDialog(Button chooseMedButton) {

        provider.getChildren(new ChildFetchListener() {
            @Override
            public void onChildrenLoaded(List<UserID> medsList) {

                if (medsList.isEmpty()) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Select Medication")
                            .setItems(new String[]{"No medications found"}, (d, w) -> {})
                            .show();
                    return;
                }

                CharSequence[] names = new CharSequence[medsList.size()];
                for (int i = 0; i < medsList.size(); i++) {
                    names[i] = medsList.get(i).name;
                }

                new AlertDialog.Builder(activity)
                        .setTitle("Select Medication")
                        .setItems(names, (dialog, which) -> {

                            UserID selectedMed = medsList.get(which);

                            chooseMedButton.setText(selectedMed.name);
                            chooseMedButton.setTag(selectedMed.uid); // store med_UUID for saving
                        })
                        .show();
            }

            @Override
            public void onError(Exception e) {
                chooseMedButton.setText("Failed to load medications");
            }
        });
    }
}