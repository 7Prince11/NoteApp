package com.kelo.noteapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteListener {

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private List<Note> notesList;
    private DatabaseHelper databaseHelper;
    private TextView emptyView;
    private FloatingActionButton fabAdd;

    private static final String CHANNEL_ID = "notes_reminder_channel";
    private static final int ADD_NOTE_REQUEST = 1;
    private static final int EDIT_NOTE_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настройка toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Инициализация views
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        fabAdd = findViewById(R.id.fabAdd);

        // Инициализация базы данных
        databaseHelper = new DatabaseHelper(this);

        // Создание канала уведомлений
        createNotificationChannel();

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Загрузка заметок
        loadNotes();

        // Обработчик кнопки добавления
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
                startActivityForResult(intent, ADD_NOTE_REQUEST);
            }
        });
    }

    private void loadNotes() {
        notesList = databaseHelper.getAllNotes();

        if (notesList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            noteAdapter = new NoteAdapter(this, notesList, this);
            recyclerView.setAdapter(noteAdapter);
        }
    }

    @Override
    public void onNoteClick(int position) {
        Note note = notesList.get(position);
        Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());
        intent.putExtra("note_reminder", note.getReminderTime());
        startActivityForResult(intent, EDIT_NOTE_REQUEST);
    }

    @Override
    public void onDeleteClick(int position) {
        Note note = notesList.get(position);

        // Отмена уведомления если есть
        if (note.getReminderTime() > 0) {
            cancelNotification(note.getId());
        }

        // Удаление из базы данных
        databaseHelper.deleteNote(note.getId());

        // Обновление списка
        notesList.remove(position);
        noteAdapter.notifyItemRemoved(position);

        // Проверка на пустой список
        if (notesList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCompleteClick(int position) {
        Note note = notesList.get(position);
        note.setCompleted(!note.isCompleted());
        databaseHelper.updateNote(note);
        noteAdapter.notifyItemChanged(position);
    }

    private void cancelNotification(int noteId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                noteId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Напоминания о заметках";
            String description = "Канал для напоминаний о задачах";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            loadNotes(); // Перезагрузка списка после добавления/редактирования
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(); // Обновление списка при возврате к активности
        updateWidget(); // Обновление виджета
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateWidget() {
        Intent intent = new Intent(this, NotesWidgetProvider.class);
        intent.setAction("com.example.notesapp.UPDATE_WIDGET");
        sendBroadcast(intent);

        // Альтернативный способ обновления виджета
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, NotesWidgetProvider.class)
        );
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            for (int appWidgetId : appWidgetIds) {
                NotesWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId);
            }
        }
    }
}