package com.kelo.noteapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteListener, CalendarAdapter.OnDateClickListener {

    // List view components
    private View listViewContainer;
    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private List<Note> notesList;
    private TextView emptyView;

    // Calendar view components
    private View calendarViewContainer;
    private RecyclerView calendarGrid;
    private CalendarAdapter calendarAdapter;
    private ImageButton btnPrevMonth, btnNextMonth;
    private TextView textMonthYear;
    private TextView textSelectedDate;
    private RecyclerView selectedDateNotes;
    private TextView emptyDateView;
    private NoteAdapter selectedDateAdapter;

    // Common components
    private DatabaseHelper databaseHelper;
    private FloatingActionButton fabAdd;
    private boolean isCalendarView = false;
    private Calendar currentCalendar;
    private MenuItem toggleViewMenuItem;

    private static final String CHANNEL_ID = "notes_reminder_channel";
    private static final int ADD_NOTE_REQUEST = 1;
    private static final int EDIT_NOTE_REQUEST = 2;
    private static final String PREFS_NAME = "NotesAppPrefs";
    private static final String PREF_VIEW_MODE = "view_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настройка toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Инициализация views
        initializeViews();

        // Инициализация базы данных
        databaseHelper = new DatabaseHelper(this);

        // Создание канала уведомлений
        createNotificationChannel();

        // Setup both views
        setupListView();
        setupCalendarView();

        // Restore view mode
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isCalendarView = prefs.getBoolean(PREF_VIEW_MODE, false);
        toggleView(isCalendarView);

        // Обработчик кнопки добавления
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
                startActivityForResult(intent, ADD_NOTE_REQUEST);
            }
        });
    }

    private void initializeViews() {
        // List view
        listViewContainer = findViewById(R.id.listViewContainer);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        // Calendar view
        calendarViewContainer = findViewById(R.id.calendarViewContainer);
        calendarGrid = findViewById(R.id.calendarGrid);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        textMonthYear = findViewById(R.id.textMonthYear);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        selectedDateNotes = findViewById(R.id.selectedDateNotes);
        emptyDateView = findViewById(R.id.emptyDateView);

        // Common
        fabAdd = findViewById(R.id.fabAdd);
    }

    private void setupListView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        setupSwipes();
        loadNotes();
    }

    private void setupCalendarView() {
        currentCalendar = Calendar.getInstance();

        // Setup calendar grid
        calendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        calendarAdapter = new CalendarAdapter(this, this);
        calendarGrid.setAdapter(calendarAdapter);

        // Setup selected date notes list
        selectedDateNotes.setLayoutManager(new LinearLayoutManager(this));

        // Month navigation
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarDisplay();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarDisplay();
        });

        updateCalendarDisplay();
    }

    private void updateCalendarDisplay() {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);

        // Update month/year header
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("ru"));
        textMonthYear.setText(monthFormat.format(currentCalendar.getTime()));

        // Get notes count for the month
        Map<String, Integer> notesCountMap = databaseHelper.getNotesCountForMonth(year, month);

        // Get recurring reminder dates for the month
        Set<String> recurringDates = databaseHelper.getRecurringDatesForMonth(year, month);

        // Update calendar grid with both regular notes and recurring reminders
        calendarAdapter.setMonth(year, month, notesCountMap, recurringDates);

        // Clear selected date
        textSelectedDate.setVisibility(View.GONE);
        selectedDateNotes.setVisibility(View.GONE);
        emptyDateView.setVisibility(View.GONE);
    }
    @Override
    public void onDateClick(int year, int month, int day) {
        // Format selected date
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, day);
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
        textSelectedDate.setText("Заметки за " + dateFormat.format(selectedDate.getTime()));
        textSelectedDate.setVisibility(View.VISIBLE);

        // Load notes for selected date
        List<Note> dateNotes = databaseHelper.getNotesForDate(year, month, day);

        if (dateNotes.isEmpty()) {
            selectedDateNotes.setVisibility(View.GONE);
            emptyDateView.setVisibility(View.VISIBLE);
        } else {
            emptyDateView.setVisibility(View.GONE);
            selectedDateNotes.setVisibility(View.VISIBLE);
            selectedDateAdapter = new NoteAdapter(this, dateNotes, this);
            selectedDateNotes.setAdapter(selectedDateAdapter);
        }
    }

    private void toggleView(boolean showCalendar) {
        isCalendarView = showCalendar;

        if (showCalendar) {
            listViewContainer.setVisibility(View.GONE);
            calendarViewContainer.setVisibility(View.VISIBLE);
            updateCalendarDisplay();
            if (toggleViewMenuItem != null) {
                toggleViewMenuItem.setIcon(R.drawable.ic_list);
            }
        } else {
            calendarViewContainer.setVisibility(View.GONE);
            listViewContainer.setVisibility(View.VISIBLE);
            loadNotes();
            if (toggleViewMenuItem != null) {
                toggleViewMenuItem.setIcon(R.drawable.ic_calendar);
            }
        }

        // Save preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_VIEW_MODE, showCalendar).apply();
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
        Note note;
        if (isCalendarView && selectedDateAdapter != null) {
            // Click from calendar date view
            note = ((NoteAdapter) selectedDateNotes.getAdapter()).notesList.get(position);
        } else {
            // Click from main list view
            note = notesList.get(position);
        }

        Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());
        intent.putExtra("note_reminder", note.getReminderTime());
        startActivityForResult(intent, EDIT_NOTE_REQUEST);
    }

    @Override
    public void onDeleteClick(int position) {
        Note note;
        RecyclerView.Adapter<?> adapter;

        if (isCalendarView && selectedDateAdapter != null) {
            note = ((NoteAdapter) selectedDateNotes.getAdapter()).notesList.get(position);
            adapter = selectedDateNotes.getAdapter();
        } else {
            note = notesList.get(position);
            adapter = noteAdapter;
        }

        // Отмена уведомления если есть
        if (note.getReminderTime() > 0) {
            cancelNotification(note.getId());
        }

        // Удаление из базы данных
        databaseHelper.deleteNote(note.getId());

        // Обновление списка
        if (isCalendarView && selectedDateAdapter != null) {
            ((NoteAdapter) adapter).notesList.remove(position);
            adapter.notifyItemRemoved(position);
            if (((NoteAdapter) adapter).notesList.isEmpty()) {
                selectedDateNotes.setVisibility(View.GONE);
                emptyDateView.setVisibility(View.VISIBLE);
            }
            // Update calendar indicators
            updateCalendarDisplay();
        } else {
            notesList.remove(position);
            noteAdapter.notifyItemRemoved(position);
            if (notesList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onCompleteClick(int position) {
        Note note;
        RecyclerView.Adapter<?> adapter;

        if (isCalendarView && selectedDateAdapter != null) {
            note = ((NoteAdapter) selectedDateNotes.getAdapter()).notesList.get(position);
            adapter = selectedDateNotes.getAdapter();
        } else {
            note = notesList.get(position);
            adapter = noteAdapter;
        }

        note.setCompleted(!note.isCompleted());
        databaseHelper.updateNote(note);
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onPinClick(int position) {
        Note note;

        if (isCalendarView && selectedDateAdapter != null) {
            note = ((NoteAdapter) selectedDateNotes.getAdapter()).notesList.get(position);
        } else {
            note = notesList.get(position);
        }

        note.setPinned(!note.isPinned());
        databaseHelper.updateNote(note);

        if (!isCalendarView) {
            // Re-sort list: pinned first, then not completed, then newest
            java.util.Collections.sort(notesList, (a,b) -> {
                if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            });
            noteAdapter.notifyDataSetChanged();
        } else if (selectedDateAdapter != null) {
            // Re-sort selected date notes
            List<Note> dateNotes = ((NoteAdapter) selectedDateNotes.getAdapter()).notesList;
            java.util.Collections.sort(dateNotes, (a,b) -> {
                if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            });
            selectedDateAdapter.notifyDataSetChanged();
        }
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
            if (isCalendarView) {
                updateCalendarDisplay();
                // Refresh selected date if any
                if (textSelectedDate.getVisibility() == View.VISIBLE) {
                    // Re-trigger date click to refresh the list
                    // You might want to store selected date and re-load it
                }
            } else {
                loadNotes();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isCalendarView) {
            updateCalendarDisplay();
        } else {
            loadNotes();
        }
        updateWidget();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        toggleViewMenuItem = menu.findItem(R.id.action_toggle_view);
        toggleViewMenuItem.setIcon(isCalendarView ? R.drawable.ic_list : R.drawable.ic_calendar);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_toggle_view) {
            toggleView(!isCalendarView);
            return true;
        } else if (id == R.id.action_search) {
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