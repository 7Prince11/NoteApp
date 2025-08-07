package com.kelo.noteapp;



import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "notes_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        int noteId = intent.getIntExtra("note_id", 0);
        String noteTitle = intent.getStringExtra("note_title");
        String noteContent = intent.getStringExtra("note_content");

        // Создание intent для открытия приложения при нажатии на уведомление
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                noteId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Звук уведомления
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Построение уведомления
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Вам нужно будет добавить эту иконку
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("Напоминание: " + noteTitle)
                .setContentText(noteContent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(noteContent))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setSound(alarmSound)
                .setVibrate(new long[]{1000, 1000, 1000, 1000})
                .setAutoCancel(true);

        // Добавление действий для Android 7.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Действие "Выполнено"
            Intent completeIntent = new Intent(context, NotificationActionReceiver.class);
            completeIntent.setAction("ACTION_COMPLETE");
            completeIntent.putExtra("note_id", noteId);

            PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                    context,
                    noteId * 10 + 1,
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder.addAction(R.drawable.ic_check, "Выполнено", completePendingIntent);

            // Действие "Отложить на 10 минут"
            Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
            snoozeIntent.setAction("ACTION_SNOOZE");
            snoozeIntent.putExtra("note_id", noteId);
            snoozeIntent.putExtra("note_title", noteTitle);
            snoozeIntent.putExtra("note_content", noteContent);

            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    noteId * 10 + 2,
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder.addAction(R.drawable.ic_snooze, "Отложить", snoozePendingIntent);
        }

        // Отображение уведомления
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(noteId, builder.build());
    }
}