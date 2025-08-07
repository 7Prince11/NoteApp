package com.kelo.noteapp;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            // Восстановление всех активных напоминаний
            restoreNotifications(context);
        }
    }

    private void restoreNotifications(Context context) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        List<Note> notesList = databaseHelper.getAllNotes();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (Note note : notesList) {
            // Проверка есть ли напоминание и не прошло ли время
            if (note.hasReminder() && !note.isReminderExpired() && !note.isCompleted()) {
                // Восстановление напоминания
                Intent notificationIntent = new Intent(context, NotificationReceiver.class);
                notificationIntent.putExtra("note_id", note.getId());
                notificationIntent.putExtra("note_title", note.getTitle());
                notificationIntent.putExtra("note_content", note.getContent());

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        note.getId(),
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Установка напоминания
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            note.getReminderTime(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            note.getReminderTime(),
                            pendingIntent
                    );
                }
            }
        }
    }
}