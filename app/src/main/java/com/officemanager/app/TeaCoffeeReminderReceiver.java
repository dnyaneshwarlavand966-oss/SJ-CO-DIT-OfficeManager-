package com.officemanager.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.Notification;

public class TeaCoffeeReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "tea_coffee_daily_reminder_v2";
    @Override public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "चहा-कॉफीची आवाजासह आठवण", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, 5108, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder notification = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        notification.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setContentTitle("चहा-कॉफीचा हिशोब")
                .setContentText("आजचा चहा आणि कॉफीचा हिशोब नोंदवला आहे का? ॲप उघडून नोंद करा.")
                .setStyle(new Notification.BigTextStyle().bigText("आजचा चहा आणि कॉफीचा हिशोब नोंदवला आहे का? ॲप उघडून कपांची संख्या आणि खर्च नोंदवा."))
                .setAutoCancel(true).setContentIntent(tap);
        if (Build.VERSION.SDK_INT < 26) notification.setPriority(Notification.PRIORITY_DEFAULT);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && (Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            manager.notify(5107, notification.build());
        }
    }
}