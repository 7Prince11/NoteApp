package com.kelo.noteapp;



import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.util.List;

public class NotesWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Создание RemoteViews для виджета
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_notes);

        // Получение последних заметок из базы данных
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        List<Note> notes = databaseHelper.getActiveNotes();

        // Очистка всех текстовых полей
        views.setTextViewText(R.id.widgetTitle, "MindStack");
        views.setTextViewText(R.id.noteTitle1, "");
        views.setTextViewText(R.id.noteTitle2, "");
        views.setTextViewText(R.id.noteTitle3, "");
        views.setTextViewText(R.id.noteContent1, "");
        views.setTextViewText(R.id.noteContent2, "");
        views.setTextViewText(R.id.noteContent3, "");

        // Установка видимости заметок
        if (notes.isEmpty()) {
            views.setTextViewText(R.id.emptyText, "Нет активных заметок");
            views.setViewVisibility(R.id.emptyText, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.note1, android.view.View.GONE);
            views.setViewVisibility(R.id.note2, android.view.View.GONE);
            views.setViewVisibility(R.id.note3, android.view.View.GONE);
        } else {
            views.setViewVisibility(R.id.emptyText, android.view.View.GONE);

            // Показать до 3 последних заметок
            if (notes.size() >= 1) {
                Note note1 = notes.get(0);
                views.setViewVisibility(R.id.note1, android.view.View.VISIBLE);
                views.setTextViewText(R.id.noteTitle1, note1.getTitle());
                views.setTextViewText(R.id.noteContent1, truncateText(note1.getContent(), 50));

                // Установка клика для первой заметки
                Intent intent1 = new Intent(context, AddEditNoteActivity.class);
                intent1.putExtra("note_id", note1.getId());
                intent1.putExtra("note_title", note1.getTitle());
                intent1.putExtra("note_content", note1.getContent());
                intent1.putExtra("note_reminder", note1.getReminderTime());
                PendingIntent pendingIntent1 = PendingIntent.getActivity(
                        context,
                        note1.getId(),
                        intent1,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                views.setOnClickPendingIntent(R.id.note1, pendingIntent1);
            } else {
                views.setViewVisibility(R.id.note1, android.view.View.GONE);
            }

            if (notes.size() >= 2) {
                Note note2 = notes.get(1);
                views.setViewVisibility(R.id.note2, android.view.View.VISIBLE);
                views.setTextViewText(R.id.noteTitle2, note2.getTitle());
                views.setTextViewText(R.id.noteContent2, truncateText(note2.getContent(), 50));

                // Установка клика для второй заметки
                Intent intent2 = new Intent(context, AddEditNoteActivity.class);
                intent2.putExtra("note_id", note2.getId());
                intent2.putExtra("note_title", note2.getTitle());
                intent2.putExtra("note_content", note2.getContent());
                intent2.putExtra("note_reminder", note2.getReminderTime());
                PendingIntent pendingIntent2 = PendingIntent.getActivity(
                        context,
                        note2.getId(),
                        intent2,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                views.setOnClickPendingIntent(R.id.note2, pendingIntent2);
            } else {
                views.setViewVisibility(R.id.note2, android.view.View.GONE);
            }

            if (notes.size() >= 3) {
                Note note3 = notes.get(2);
                views.setViewVisibility(R.id.note3, android.view.View.VISIBLE);
                views.setTextViewText(R.id.noteTitle3, note3.getTitle());
                views.setTextViewText(R.id.noteContent3, truncateText(note3.getContent(), 50));

                // Установка клика для третьей заметки
                Intent intent3 = new Intent(context, AddEditNoteActivity.class);
                intent3.putExtra("note_id", note3.getId());
                intent3.putExtra("note_title", note3.getTitle());
                intent3.putExtra("note_content", note3.getContent());
                intent3.putExtra("note_reminder", note3.getReminderTime());
                PendingIntent pendingIntent3 = PendingIntent.getActivity(
                        context,
                        note3.getId(),
                        intent3,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                views.setOnClickPendingIntent(R.id.note3, pendingIntent3);
            } else {
                views.setViewVisibility(R.id.note3, android.view.View.GONE);
            }
        }

        // Установка клика на заголовок виджета для открытия приложения
        Intent appIntent = new Intent(context, MainActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetHeader, appPendingIntent);

        // Установка клика на кнопку добавления
        Intent addIntent = new Intent(context, AddEditNoteActivity.class);
        PendingIntent addPendingIntent = PendingIntent.getActivity(
                context,
                -1,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetAddButton, addPendingIntent);

        // Обновление виджета
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    @Override
    public void onEnabled(Context context) {
        // Вызывается при добавлении первого экземпляра виджета
    }

    @Override
    public void onDisabled(Context context) {
        // Вызывается при удалении последнего экземпляра виджета
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Обновление виджета при изменении данных
        if ("com.example.notesapp.UPDATE_WIDGET".equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new android.content.ComponentName(context, NotesWidgetProvider.class)
            );
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}