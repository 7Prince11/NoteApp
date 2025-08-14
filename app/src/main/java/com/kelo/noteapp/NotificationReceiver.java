package com.kelo.noteapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "note_channel";

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void onReceive(Context context, Intent intent) {
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

        createChannel(context);

        // Create intent to open the specific note when notification is clicked
        Intent notificationIntent = new Intent(context, AddEditNoteActivity.class);
        notificationIntent.putExtra("note_id", noteId);
        notificationIntent.putExtra("note_title", title);
        notificationIntent.putExtra("note_content", content);
        notificationIntent.putExtra("note_reminder", note.getReminderTime());
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                noteId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create "Done" action
        Intent doneIntent = new Intent(context, NotificationActionReceiver.class);
        doneIntent.setAction("ACTION_COMPLETE");
        doneIntent.putExtra("note_id", noteId);
        doneIntent.putExtra("note_title", title);
        doneIntent.putExtra("note_content", content);

        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context,
                noteId + 1000, // Different request code
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create "Snooze" action
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction("ACTION_SNOOZE");
        snoozeIntent.putExtra("note_id", noteId);
        snoozeIntent.putExtra("note_title", title);
        snoozeIntent.putExtra("note_content", content);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                noteId + 2000, // Different request code
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification with enhanced features
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle(title != null ? title : "Напоминание")
                .setContentText(content != null && !content.isEmpty() ? content : "Не забудьте проверить заметку")
                .setContentIntent(contentPendingIntent) // Make notification clickable
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Add action buttons
                .addAction(R.drawable.ic_check, "Выполнено", donePendingIntent)
                .addAction(R.drawable.ic_snooze, "Отложить", snoozePendingIntent)
                // Enhanced styling
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(content != null && !content.isEmpty() ? content : "Не забудьте проверить заметку")
                        .setBigContentTitle(title != null ? title : "Напоминание"))
                .setColor(context.getResources().getColor(R.color.colorPrimary));

        // Show the notification
        NotificationManagerCompat.from(context).notify(noteId, builder.build());

        // Reschedule next occurrence if repeating
        if (note.getRepeatDays() != 0) {
            long next = computeNextOccurrenceFromNow(note.getRepeatDays(),
                    note.getReminderTime()); // extract hour/min from stored reminderTime
            // Update stored time and reschedule
            note.setReminderTime(next);
            databaseHelper.updateNote(note);
            schedule(context, note);
        }
    }

    private static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания о заметках",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Канал для напоминаний о задачах в приложении заметок");
            ch.enableVibration(true);
            ch.enableLights(true);
            ch.setShowBadge(true);
            ch.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private static void schedule(Context context, Note note) {
        Intent i = new Intent(context, NotificationReceiver.class);
        i.putExtra("note_id", note.getId());
        i.putExtra("note_title", note.getTitle());
        i.putExtra("note_content", note.getContent());

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                note.getId(),
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            long when = note.getReminderTime();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
            }
        }
    }

    // Compute next trigger from NOW using the hour/minute taken from last reminderTime
    private static long computeNextOccurrenceFromNow(int mask, long lastReminderTime) {
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastReminderTime);
        int hour = last.get(Calendar.HOUR_OF_DAY);
        int minute = last.get(Calendar.MINUTE);
        return computeNextOccurrenceFromNow(mask, hour, minute);
    }

    // Mon=0..Sun=6
    private static int todayIndex(Calendar cal) {
        int dow = cal.get(Calendar.DAY_OF_WEEK); // Sun=1..Sat=7
        return (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY); // Mon->0 ... Sun->6
    }

    private static boolean isBitSet(int mask, int idx) {
        return ((mask >> idx) & 1) == 1;
    }

    private static long computeNextOccurrenceFromNow(int mask, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar cand = (Calendar) now.clone();
        cand.set(Calendar.HOUR_OF_DAY, hour);
        cand.set(Calendar.MINUTE, minute);
        cand.set(Calendar.SECOND, 0);
        cand.set(Calendar.MILLISECOND, 0);

        int todayIdx = todayIndex(now);
        for (int offset = 0; offset < 7; offset++) {
            int idx = (todayIdx + offset) % 7;
            if (!isBitSet(mask, idx)) continue;

            Calendar test = (Calendar) cand.clone();
            test.add(Calendar.DAY_OF_MONTH, offset);
            if (test.getTimeInMillis() > now.getTimeInMillis()) {
                return test.getTimeInMillis();
            }
        }
        // Fallback (не должно дойти)
        return now.getTimeInMillis() + 60_000;
    }

    // Public method to cancel notification
    public static void cancelNotification(Context context, int noteId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(noteId);
        }
    }
}