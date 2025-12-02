package com.example.smartairsetup.notification;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.smartairsetup.notification.NotificationReceiver;

public class AlertHelper {

    /**
     * To send alert
     * Just call: NotificationHelper.showAlert(context, type, message)
     */
    public static void showAlert(Context context, String type, String message) {

        String title = selectTitle(type);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EXTRA_TITLE, title);
        intent.putExtra(NotificationReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(NotificationReceiver.EXTRA_ID, (int) System.currentTimeMillis());

        context.sendBroadcast(intent);
    }
    private static String selectTitle(String type) {
        String title;
        if ("TRIAGE_START".equals(type)) {
            title = "Triage started";
        } else if ("TRIAGE_ESCALATION".equals(type)) {
            title = "Triage escalation";
        } else if ("RED_ZONE".equals(type)) {
            title = "Red-zone day";
        } else if ("RESCUE_REPEATED".equals(type)) {
            title = "Frequent rescue use - Check on your child";
        } else if ("INVENTORY_LOW".equals(type)) {
            title = "Medication inventory low";
        } else if ("WORSE_AFTER_DOSE".equals(type)) {
            title = "Symptoms worse after dose";
        } else {
            title = "SmartAir alert";
        }
        return title;
    }

    public static void sendAlertToParent(String parentUid, String childId, String type, Activity passedActivity){

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(passedActivity, "Please add a child first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (parentUid == null || parentUid.isEmpty()) {
            Toast.makeText(passedActivity, "Parent account not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        sendAlertToFirestore(parentUid, childId, type, passedActivity);

        /*
        //sends a local notification you can see - useful for testing solo
        if (!NotificationPermissionsHelper.ensureNotificationPermissions(this)) {
            return;
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EXTRA_TITLE, "Triage Alert");
        intent.putExtra(NotificationReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(NotificationReceiver.EXTRA_ID, (int) System.currentTimeMillis());
        sendBroadcast(intent);

         */

    }

    private static void sendAlertToFirestore(String parentUid, String childId, String type, Activity passedActivity){

        // 1) Send alert to Firestore (cloud)
        AlertRepository alertRepo = new AlertRepository();
        String message = "Your child has requested help! See title of notification";

        alertRepo.sendAlert(
                parentUid,
                childId, type,
                message,
                aVoid -> Toast.makeText(passedActivity, "Alert sent to parent.", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(passedActivity, "Failed to send alert: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );

    }

}
