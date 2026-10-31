package com.officemanager.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;

public final class ReminderScheduler {
    private static final String PREFS = "office_reminders";
    private static final String HOUR = "tea_hour";
    private static final String MINUTE = "tea_minute";
    private static final String ENABLED = "tea_enabled";
    private ReminderScheduler() {}

    public static void scheduleDaily(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(ENABLED, false)) {
            scheduleAlarm(context, prefs.getInt(HOUR, 17), prefs.getInt(MINUTE, 0));
        } else {
            cancelAlarm(context);
        }
    }

    public static boolean scheduleDaily(Context context, int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return false;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(HOUR, hour).putInt(MINUTE, minute).putBoolean(ENABLED, true).apply();
        return scheduleAlarm(context, hour, minute);
    }

    public static void cancelDaily(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, false).apply();
        cancelAlarm(context);
    }

    private static void cancelAlarm(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        Intent intent = new Intent(context, TeaCoffeeReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 5107, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pending);
    }

    private static boolean scheduleAlarm(Context context, int hour, int minute) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return false;
        Intent intent = new Intent(context, TeaCoffeeReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 5107, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) next.add(Calendar.DAY_OF_YEAR, 1);
        manager.cancel(pending);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pending);
        return true;
    }
}