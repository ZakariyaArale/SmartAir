package com.example.smartairsetup;

import android.app.Activity;
import android.widget.Button;
import android.app.AlertDialog;

//This is responsible for updating the UI when a parent select a child
public class ChildDiaglog {
    private final Activity activity;

    //TODO: Make a list of children from firebase
    private final String[] children = {"Alice", "Bob", "Charlie"};

    public ChildDiaglog(Activity activity) {
        this.activity = activity;
    }

    public void showSelectionDialog(Button chooseChildButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select a child");
        builder.setItems(children, (dialog, which) -> {
            chooseChildButton.setText(children[which]);
        });
        builder.show();
    }
}
