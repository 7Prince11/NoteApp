package com.kelo.noteapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Canvas;
import androidx.cardview.widget.CardView;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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

    // Notification synchronization
    private BroadcastReceiver noteUpdateReceiver;

    // Sort functionality
    private int currentSortMode = 0; // 0 = Default (Date newest), 1 = Date oldest, 2 = Category, 3 = Title
    private static final String PREF_SORT_MODE = "sort_mode";

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

        // Restore view mode and sort mode
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isCalendarView = prefs.getBoolean(PREF_VIEW_MODE, false);
        currentSortMode = prefs.getInt(PREF_SORT_MODE, 0); // Default sort
        toggleView(isCalendarView);

        // Setup notification synchronization
        setupBroadcastReceiver();

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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void setupBroadcastReceiver() {
        noteUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.kelo.noteapp.NOTE_UPDATED".equals(intent.getAction())) {
                    int noteId = intent.getIntExtra("note_id", -1);
                    String action = intent.getStringExtra("action");

                    if ("completed".equals(action)) {
                        // Refresh the list to show completed status
                        if (isCalendarView) {
                            updateCalendarDisplay();
                        } else {
                            loadNotes();
                        }

                        // Show feedback to user
                        if (fabAdd != null) {
                            Snackbar.make(fabAdd, "Задача выполнена из уведомления", Snackbar.LENGTH_SHORT).show();
                        }
                    } else if ("snoozed".equals(action)) {
                        // Show feedback that note was snoozed
                        if (fabAdd != null) {
                            Snackbar.make(fabAdd, "Напоминание отложено на 10 минут", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.kelo.noteapp.NOTE_UPDATED");

        // FIX: Add RECEIVER_NOT_EXPORTED flag for Android 13+ compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(noteUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(noteUpdateReceiver, filter);
        }
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

    // NEW: Show custom sort dialog
    private void showCustomSortDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sort_notes);

        // Make dialog take full width with some margin
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Get views from dialog
        CardView cardDateNewest = dialog.findViewById(R.id.cardDateNewest);
        CardView cardDateOldest = dialog.findViewById(R.id.cardDateOldest);
        CardView cardCategory = dialog.findViewById(R.id.cardCategory);
        CardView cardTitle = dialog.findViewById(R.id.cardTitle);

        ImageView checkDateNewest = dialog.findViewById(R.id.checkDateNewest);
        ImageView checkDateOldest = dialog.findViewById(R.id.checkDateOldest);
        ImageView checkCategory = dialog.findViewById(R.id.checkCategory);
        ImageView checkTitle = dialog.findViewById(R.id.checkTitle);

        ImageButton btnCloseDialog = dialog.findViewById(R.id.btnCloseDialog);

        // Set current selection
        updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle);

        // Close button
        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        // Sort option clicks
        cardDateNewest.setOnClickListener(v -> {
            setSortMode(0, dialog);
        });

        cardDateOldest.setOnClickListener(v -> {
            setSortMode(1, dialog);
        });

        cardCategory.setOnClickListener(v -> {
            setSortMode(2, dialog);
        });

        cardTitle.setOnClickListener(v -> {
            setSortMode(3, dialog);
        });

        dialog.show();
    }

    // NEW: Update visual selection in sort dialog
    private void updateSortDialogSelection(ImageView checkDateNewest, ImageView checkDateOldest,
                                           ImageView checkCategory, ImageView checkTitle) {
        // Hide all checkmarks
        checkDateNewest.setVisibility(View.GONE);
        checkDateOldest.setVisibility(View.GONE);
        checkCategory.setVisibility(View.GONE);
        checkTitle.setVisibility(View.GONE);

        // Show checkmark for current selection
        switch (currentSortMode) {
            case 0:
                checkDateNewest.setVisibility(View.VISIBLE);
                break;
            case 1:
                checkDateOldest.setVisibility(View.VISIBLE);
                break;
            case 2:
                checkCategory.setVisibility(View.VISIBLE);
                break;
            case 3:
                checkTitle.setVisibility(View.VISIBLE);
                break;
        }
    }

    // NEW: Set sort mode and apply
    private void setSortMode(int sortMode, Dialog dialog) {
        currentSortMode = sortMode;

        // Save sort preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(PREF_SORT_MODE, currentSortMode).apply();

        // Apply sort and refresh list
        loadNotes();
        dialog.dismiss();
    }

    // Sort notes based on current sort mode
    private void sortNotes(List<Note> notes) {
        switch (currentSortMode) {
            case 0: // Date newest first (default)
                Collections.sort(notes, new Comparator<Note>() {
                    @Override
                    public int compare(Note a, Note b) {
                        // Pinned notes first
                        if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                        // Then by completion status
                        if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                        // Then by date (newest first)
                        return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                    }
                });
                break;

            case 1: // Date oldest first
                Collections.sort(notes, new Comparator<Note>() {
                    @Override
                    public int compare(Note a, Note b) {
                        // Pinned notes first
                        if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                        // Then by completion status
                        if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                        // Then by date (oldest first)
                        return Long.compare(a.getCreatedAt(), b.getCreatedAt());
                    }
                });
                break;

            case 2: // By category
                Collections.sort(notes, new Comparator<Note>() {
                    @Override
                    public int compare(Note a, Note b) {
                        // Pinned notes first
                        if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                        // Then by completion status
                        if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                        // Then by category
                        String catA = a.getCategory() != null ? a.getCategory() : "personal";
                        String catB = b.getCategory() != null ? b.getCategory() : "personal";
                        int categoryCompare = catA.compareTo(catB);
                        if (categoryCompare != 0) return categoryCompare;
                        // Then by date (newest first) within same category
                        return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                    }
                });
                break;

            case 3: // By title alphabetically
                Collections.sort(notes, new Comparator<Note>() {
                    @Override
                    public int compare(Note a, Note b) {
                        // Pinned notes first
                        if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                        // Then by completion status
                        if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                        // Then by title alphabetically
                        return a.getTitle().compareToIgnoreCase(b.getTitle());
                    }
                });
                break;
        }
    }

    private void loadNotes() {
        notesList = databaseHelper.getAllNotes();

        // Apply current sort mode
        sortNotes(notesList);

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

        // ENHANCED: Cancel notification AND alarm for this note
        if (note.getReminderTime() > 0) {
            cancelNotification(note.getId());
            // Also cancel the notification if it's currently showing
            NotificationReceiver.cancelNotification(this, note.getId());
        }

        // Delete from database
        databaseHelper.deleteNote(note.getId());

        // Update the list
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

        updateWidget();
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

        // If note is being marked as completed, cancel its notification
        if (note.isCompleted() && note.getReminderTime() > 0) {
            cancelNotification(note.getId());
            NotificationReceiver.cancelNotification(this, note.getId());
        }

        databaseHelper.updateNote(note);
        adapter.notifyItemChanged(position);
        updateWidget();
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
            // Re-sort list with current sort mode
            sortNotes(notesList);
            noteAdapter.notifyDataSetChanged();
        } else if (selectedDateAdapter != null) {
            // Re-sort selected date notes
            List<Note> dateNotes = ((NoteAdapter) selectedDateNotes.getAdapter()).notesList;
            sortNotes(dateNotes);
            selectedDateAdapter.notifyDataSetChanged();
        }
    }

    // Enhanced method for better notification cancellation
    private void cancelNotification(int noteId) {
        // Cancel the alarm
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                noteId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        // Cancel any currently showing notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(noteId);
        }
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
    protected void onDestroy() {
        super.onDestroy();
        if (noteUpdateReceiver != null) {
            unregisterReceiver(noteUpdateReceiver);
        }
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

        if (id == R.id.action_sort) {
            // NEW: Show custom sort dialog
            showCustomSortDialog();
            return true;
        } else if (id == R.id.action_toggle_view) {
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
                                NotificationReceiver.cancelNotification(MainActivity.this, note.getId());
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

                            // If note is being marked as completed, cancel its notification
                            if (note.isCompleted() && note.getReminderTime() > 0) {
                                cancelNotification(note.getId());
                                NotificationReceiver.cancelNotification(MainActivity.this, note.getId());
                            }

                            databaseHelper.updateNote(note);
                            noteAdapter.notifyItemChanged(pos);
                            updateWidget();
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