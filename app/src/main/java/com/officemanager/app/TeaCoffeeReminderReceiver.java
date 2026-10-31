package com.officemanager.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.Notification;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

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
        speakReminder(context);
    }

    private void speakReminder(Context context) {
        final PendingResult pending = goAsync();
        final AtomicBoolean finished = new AtomicBoolean(false);
        final TextToSpeech[] engine = new TextToSpeech[1];
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable finish = () -> {
            if (finished.compareAndSet(false, true)) {
                if (engine[0] != null) engine[0].shutdown();
                pending.finish();
            }
        };
        handler.postDelayed(finish, 12000L);
        try {
            engine[0] = new TextToSpeech(context.getApplicationContext(), status -> {
                if (finished.get()) return;
                if (status != TextToSpeech.SUCCESS || engine[0] == null) {
                    finish.run();
                    return;
                }
                int language = engine[0].setLanguage(new Locale("mr", "IN"));
                if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    language = engine[0].setLanguage(new Locale("hi", "IN"));
                }
                if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    finish.run();
                    return;
                }
                engine[0].setSpeechRate(0.9f);
                engine[0].setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {}
                    @Override public void onDone(String utteranceId) { handler.post(finish); }
                    @Override public void onError(String utteranceId) { handler.post(finish); }
                });
                Bundle params = new Bundle();
                engine[0].speak("संध्याकाळचे पाच वाजले आहेत. चहा आणि कॉफीचा हिशोब नोंदवा.", TextToSpeech.QUEUE_FLUSH, params, "office-tea-reminder");
            });
        } catch (Exception e) {
            finish.run();
        }
    }
}