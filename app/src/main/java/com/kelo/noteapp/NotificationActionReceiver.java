package com.kelo.noteapp;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;


public class NotificationActionReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "note_channel";


    @Override
    public void onReceive(Context context, Intent intent) {
        // Check notification permission first (API 33+)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                return; // No permission, don't show notification
            }
        }

        int noteId = intent.getIntExtra("note_id", -1);
        String title = intent.getStringExtra("note_title");
        String content = intent.getStringExtra("note_content");

        // Check if note still exists in database
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        Note note = databaseHelper.getNote(noteId);

        // If note doesn't exist or is completed, don't show notification
        if (note == null || note.isCompleted()) {
            return;
        }

        // ... rest of your existing code remains the same
    }

    private void handleCompleteAction(Context context, int noteId, String noteTitle) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        Note note = databaseHelper.getNote(noteId);

        if (note != null) {
            // Mark note as completed
            note.setCompleted(true);
            databaseHelper.updateNote(note);

            // Cancel any future alarms for this note
            cancelAlarm(context, noteId);

            // Show success notification
            showActionFeedback(context, "✅ " + (noteTitle != null ? noteTitle : "Задача") + " выполнена!",
                    "Заметка отмечена как завершенная", noteId + 10000);

            // Send broadcast to update UI if app is open
            Intent updateIntent = new Intent("com.kelo.noteapp.NOTE_UPDATED");
            updateIntent.putExtra("note_id", noteId);
            updateIntent.putExtra("action", "completed");
            context.sendBroadcast(updateIntent);
        }
    }

    private void handleSnoozeAction(Context context, int noteId, String noteTitle, String noteContent) {
        // Snooze for 10 minutes
        long snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000);

        // Create new alarm for snoozed notification
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
        if (alarmManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        pendingIntent
                );
            }
        }

        // Show snooze confirmation
        showActionFeedback(context, "⏰ Напоминание отложено",
                "Напомним через 10 минут", noteId + 20000);

        // Send broadcast to update UI if app is open
        Intent updateIntent = new Intent("com.kelo.noteapp.NOTE_UPDATED");
        updateIntent.putExtra("note_id", noteId);
        updateIntent.putExtra("action", "snoozed");
        context.sendBroadcast(updateIntent);
    }

    private void cancelAlarm(Context context, int noteId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                noteId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
    private void showActionFeedback(Context context, String title, String message, int notificationId) {
        // Check if notification permission is granted (API 33+)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                // Just show toast if no permission
                Toast.makeText(context, title, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create a simple feedback notification that auto-dismisses
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(3000) // Auto dismiss after 3 seconds
                .setColor(context.getResources().getColor(R.color.success_color));

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            // Fallback to toast if notification fails
            Toast.makeText(context, title, Toast.LENGTH_SHORT).show();
        }

        // Also show a toast for immediate feedback
        Toast.makeText(context, title, Toast.LENGTH_SHORT).show();
    }
}