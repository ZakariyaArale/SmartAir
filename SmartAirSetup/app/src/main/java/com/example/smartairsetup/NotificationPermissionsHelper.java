package com.example.smartairsetup;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.NotificationManagerCompat;

public class NotificationPermissionsHelper {
    public static boolean ensureNotificationPermissions(Activity activity) {

        // Check notification permission
        NotificationManagerCompat manager = NotificationManagerCompat.from(activity);
        // a class that helps deal with notifications over many API's
        if (!manager.areNotificationsEnabled()) { //checks if they have notifications enabled

            showPermissionDialog(activity, "notifications");
        }
        return  manager.areNotificationsEnabled();  //I need to figure out call back
        // so this will work. At this point it always returns false even if user updates settings////////////////////////////////////////////////////////////
    }

    public static boolean ensureAlarmPermissions(Activity activity){

        // Check alarm permission
        //Makes sure it doesn't call newer API on older devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //references the systems Alarm Manager services
            AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) { //checks if EXACT alarm permissions are on

                //sends dialog to ask if they want permissions changed, returns T/F depending
                // on user choice
                showPermissionDialog(activity, "exact_alarms");
            }
            return alarmManager.canScheduleExactAlarms();/////////////////////////////////////////////////////////Update this as well

        }
        return false;  //returns false if API isn't high enough version... should I handle this better???????
    }




    //helper function that makes a popup explaining why we need notification/alarm access
    private static void showPermissionDialog(Activity activity, String type) {
        String message;
        //initializes message based on which pop up I want.
        if (type.equals("exact_alarms")) {
            message = "This app needs exact alarm permission to send timely medication reminders.";
        } else {
            message = "This app needs notification permission to alert you about medications.";
        }

        //makes an android pop-up
        new AlertDialog.Builder(activity)
                .setTitle("Permission Needed") //set's title of pop-up
                .setMessage(message) //sets message to desired message

                //makes a button that will direct them to settings
                .setPositiveButton("Go to Settings", (dialog, which) -> {

                    //chooses appropriate settings screen depending on type
                    Intent intent;
                    if (type.equals("exact_alarms") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    } else {
                        intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                    }
                    activity.startActivity(intent); //sends them to the settings page
                })
                //allows user to leave notification set up till later
                .setNegativeButton("Maybe Later", (dialog, which) -> dialog.dismiss())

                .setCancelable(false) //doesn't allow user to click out of the pop-up
                // by clicking elsewhere
                .show(); //makes constructed pop-up visible
    }


}
