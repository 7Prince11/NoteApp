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
import androidx.core.content.ContextCompat;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Canvas;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.ItemTouchHelper;
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

        setupSwipes();

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
        // sort: pinned → not completed → newest
        java.util.Collections.sort(notesList, (a,b) -> {
            if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
            if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
            return Long.compare(b.getCreatedAt(), a.getCreatedAt());
        });
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

    private void setupSwipes() {
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                        View itemView = viewHolder.itemView;
                        float height = (float) itemView.getBottom() - itemView.getTop();
                        float iconSize = height * 0.4f;

                        Paint paint = new Paint();
                        Drawable icon;

                        if (dX > 0) {
                            // Swiping right -> complete (green background, check icon)
                            paint.setColor(Color.parseColor("#2E7D32")); // green 800
                            c.drawRect(itemView.getLeft(), itemView.getTop(),
                                    itemView.getLeft() + dX, itemView.getBottom(), paint);

                            icon = ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.ic_check_green);
                            if (icon != null) {
                                int left = itemView.getLeft() + (int)(height * 0.3f);
                                int top = itemView.getTop() + (int)((height - iconSize) / 2);
                                int right = left + (int)iconSize;
                                int bottom = top + (int)iconSize;
                                icon.setBounds(left, top, right, bottom);
                                icon.draw(c);
                            }
                        } else if (dX < 0) {
                            // Swiping left -> delete (red background, trash icon)
                            paint.setColor(Color.parseColor("#C62828")); // red 800
                            c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                                    itemView.getRight(), itemView.getBottom(), paint);

                            icon = ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.ic_delete);
                            if (icon != null) {
                                int right = itemView.getRight() - (int)(height * 0.3f);
                                int top = itemView.getTop() + (int)((height - iconSize) / 2);
                                int left = right - (int)iconSize;
                                int bottom = top + (int)iconSize;
                                icon.setBounds(left, top, right, bottom);
                                icon.draw(c);
                            }
                        }
                    }
                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        int pos = viewHolder.getBindingAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;

                        Note note = notesList.get(pos);

                        if (direction == ItemTouchHelper.LEFT) {
                            // Delete with UNDO
                            // cancel reminder if any
                            if (note.getReminderTime() > 0) {
                                cancelNotification(note.getId());
                            }
                            // remove from DB & list
                            databaseHelper.deleteNote(note.getId());
                            notesList.remove(pos);
                            noteAdapter.notifyItemRemoved(pos);

                            // empty state toggle
                            if (notesList.isEmpty()) {
                                recyclerView.setVisibility(View.GONE);
                                emptyView.setVisibility(View.VISIBLE);
                            }

                            updateWidget();

                            Snackbar.make(MainActivity.this.recyclerView, "Note deleted", Snackbar.LENGTH_LONG)
                                    .setAction("UNDO", v -> {
                                        long newId = databaseHelper.addNote(note);
                                        note.setId((int) newId);
                                        notesList.add(pos, note);
                                        noteAdapter.notifyItemInserted(pos);

                                        // reschedule reminder if exists
                                        if (note.getReminderTime() > 0) {
                                            scheduleNotification(note);
                                        }

                                        recyclerView.setVisibility(View.VISIBLE);
                                        emptyView.setVisibility(View.GONE);
                                        updateWidget();
                                    })
                                    .addCallback(new Snackbar.Callback() {
                                        @Override
                                        public void onDismissed(Snackbar transientBottomBar, int event) {
                                            // refresh row if user dismissed without UNDO to clear swipe state
                                            noteAdapter.notifyItemRangeChanged(pos, noteAdapter.getItemCount() - pos);
                                        }
                                    })
                                    .show();

                        } else if (direction == ItemTouchHelper.RIGHT) {
                            // Toggle complete
                            note.setCompleted(!note.isCompleted());
                            databaseHelper.updateNote(note);
                            noteAdapter.notifyItemChanged(pos);
                        }
                    }
                };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    @Override
    public void onPinClick(int position) {
        Note note = notesList.get(position);
        note.setPinned(!note.isPinned());
        databaseHelper.updateNote(note);

        // Re-sort list: pinned first, then not completed, then newest
        java.util.Collections.sort(notesList, (a,b) -> {
            if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
            if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
            return Long.compare(b.getCreatedAt(), a.getCreatedAt());
        });

        noteAdapter.notifyDataSetChanged();
        // (optional) update widget if you want immediate reflect
        // updateWidget();
    }
    private void scheduleNotification(Note note) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                note.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long when = note.getReminderTime();
        if (when <= 0) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        }
    }

}
