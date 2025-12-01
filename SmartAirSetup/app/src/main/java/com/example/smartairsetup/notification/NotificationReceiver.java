package com.example.smartairsetup.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "reminder_channel";
    private static final int NOTIFICATION_ID = 1;

    //extras so we can customize alerts
    public static final String EXTRA_TITLE = "extra_notification_title";
    public static final String EXTRA_MESSAGE = "extra_notification_message";
    public static final String EXTRA_ID = "extra_notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannelIfNeeded(context);

        // Starting with Android 13 you must have POST_NOTIFICATIONS permission.
        // Check permission and bail out if not granted.
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // We can't request permission from a BroadcastReceiver, so just don't show it.
            return;
        }

        // Read optional title/message/id from extras.
        String title = intent.getStringExtra(EXTRA_TITLE);
        if (title == null || title.isEmpty()) {
            title = "Fallback Reminder"; // fallback
        }

        String message = intent.getStringExtra(EXTRA_MESSAGE);
        if (message == null || message.isEmpty()) {
            message = "Your scheduled reminder is here!"; // fallback
        }

        int notificationId = intent.getIntExtra(EXTRA_ID, NOTIFICATION_ID);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.notify(notificationId, builder.build());
    }

    private void createNotificationChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Reminders & Alerts";
            String description = "Channel for medication reminders and safety alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}