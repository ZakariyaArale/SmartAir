package com.example.smartairsetup.pef;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.app.AlertDialog;

import com.example.smartairsetup.zone.ZoneActivity;

import java.util.List;

public class ChildDiaglog {

    private final Activity activity;
    private final ProcessChildren provider;

    public ChildDiaglog(Activity activity, ProcessChildren provider) {
        this.activity = activity;
        this.provider = provider;
    }

    public void showSelectionDialog(Button chooseChildButton) {

        provider.getChildren(new ChildFetchListener() {
            @Override
            public void onChildrenLoaded(List<UserID> childrenList) {

                if (childrenList.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("Select a child");
                    builder.setItems(new String[]{"No children found"}, (dialog, which) -> {});
                    builder.show();
                    return;
                }

                CharSequence[] names = new CharSequence[childrenList.size()];
                for (int i = 0; i < childrenList.size(); i++) {
                    names[i] = childrenList.get(i).name;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Select a child");
                builder.setItems(names, (dialog, which) -> {

                    UserID selectedChild = childrenList.get(which);

                    chooseChildButton.setText(selectedChild.name);
                    chooseChildButton.setTag(selectedChild.uid);

                    // Immediately update zone color after selection
                    if (activity instanceof ZoneActivity) {
                        GradientDrawable background =
                                (GradientDrawable) ((ZoneActivity) activity).zoneLabel.getBackground();
                        ((ZoneActivity) activity).updateZoneColor(selectedChild.uid, background);
                    }
                });

                builder.show();
            }

            @Override
            public void onError(Exception e) {
                chooseChildButton.setText("Failed to load children");
            }
        });
    }
}