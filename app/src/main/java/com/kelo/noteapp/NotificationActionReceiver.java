package com.kelo.noteapp;


import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int noteId = intent.getIntExtra("note_id", -1);

        // Закрытие уведомления
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(noteId);

        if ("ACTION_COMPLETE".equals(action)) {
            // Отметить задачу как выполненную
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            Note note = databaseHelper.getNote(noteId);

            if (note != null) {
                note.setCompleted(true);
                databaseHelper.updateNote(note);

                Toast.makeText(context, "Задача выполнена!", Toast.LENGTH_SHORT).show();
            }

        } else if ("ACTION_SNOOZE".equals(action)) {
            // Отложить напоминание на 10 минут
            String noteTitle = intent.getStringExtra("note_title");
            String noteContent = intent.getStringExtra("note_content");

            Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
            snoozeIntent.putExtra("note_id", noteId);
            snoozeIntent.putExtra("note_title", noteTitle);
            snoozeIntent.putExtra("note_content", noteContent);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    noteId,
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            long triggerTime = System.currentTimeMillis() + (10 * 60 * 1000); // 10 минут

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }

            Toast.makeText(context, "Напоминание отложено на 10 минут", Toast.LENGTH_SHORT).show();
        }
    }
}