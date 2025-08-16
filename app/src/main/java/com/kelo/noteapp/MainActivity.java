package com.kelo.noteapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
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
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
                startActivityForResult(intent, ADD_NOTE_REQUEST);
            }
        });

        // Handle intent from notification
        handleNotificationIntent(getIntent());
    }

    private void initializeViews() {
        // List view components
        listViewContainer = findViewById(R.id.listViewContainer);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        // Calendar view components
        calendarViewContainer = findViewById(R.id.calendarViewContainer);
        calendarGrid = findViewById(R.id.calendarGrid);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        textMonthYear = findViewById(R.id.textMonthYear);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        selectedDateNotes = findViewById(R.id.selectedDateNotes);
        emptyDateView = findViewById(R.id.emptyDateView);

        // Common components
        fabAdd = findViewById(R.id.fabAdd);

        // Initialize calendar
        currentCalendar = Calendar.getInstance();
    }

    private void setupListView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Initialize empty list to avoid null pointer
        notesList = new ArrayList<>();
        noteAdapter = new NoteAdapter(this, notesList, this);
        recyclerView.setAdapter(noteAdapter);

        // Add swipe to delete functionality
        setupSwipeToDelete();
    }

    private void setupCalendarView() {
        // Setup calendar grid
        calendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        calendarAdapter = new CalendarAdapter(this, this);
        calendarGrid.setAdapter(calendarAdapter);

        // Setup selected date notes list
        selectedDateNotes.setLayoutManager(new LinearLayoutManager(this));

        // Navigation buttons
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarDisplay();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarDisplay();
        });

        // Initialize calendar display
        updateCalendarDisplay();
    }

    private void updateCalendarDisplay() {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);

        // Update month/year header
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("ru"));
        textMonthYear.setText(monthFormat.format(currentCalendar.getTime()));

        // Get notes count for the month (now includes 7-day rolling window for recurring)
        Map<String, Integer> notesCountMap = databaseHelper.getNotesCountForMonth(year, month);

        // Get recurring reminder dates for the month (now limited to 7 days)
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

        // Load notes for selected date (includes recurring notes within 7-day window)
        List<Note> dateNotes = databaseHelper.getNotesForDate(year, month, day);

        if (dateNotes.isEmpty()) {
            selectedDateNotes.setVisibility(View.GONE);
            emptyDateView.setVisibility(View.VISIBLE);
            emptyDateView.setText("Нет заметок на выбранную дату");
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

    private void setupSwipeToDelete() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Note note = notesList.get(position);

                // Move to trash instead of permanent delete
                databaseHelper.moveToTrash(note.getId());
                notesList.remove(position);
                noteAdapter.notifyItemRemoved(position);

                // Show undo snackbar
                Snackbar.make(recyclerView, "Заметка перемещена в корзину", Snackbar.LENGTH_LONG)
                        .setAction("ОТМЕНИТЬ", v -> {
                            databaseHelper.restoreFromTrash(note.getId());
                            notesList.add(position, note);
                            noteAdapter.notifyItemInserted(position);
                        })
                        .show();

                updateEmptyState();
                updateAppWidget();
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                    paint.setColor(ContextCompat.getColor(MainActivity.this, R.color.delete_color));

                    // Draw background
                    if (dX > 0) {
                        c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), paint);
                    } else {
                        c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), paint);
                    }

                    // Draw delete icon
                    Drawable icon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_delete);
                    if (icon != null) {
                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();

                        if (dX > 0) {
                            int iconLeft = itemView.getLeft() + iconMargin;
                            int iconRight = iconLeft + icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        } else {
                            int iconRight = itemView.getRight() - iconMargin;
                            int iconLeft = iconRight - icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        }
                        icon.setTint(Color.WHITE);
                        icon.draw(c);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }


    private void setupBroadcastReceiver() {
        noteUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.kelo.noteapp.UPDATE_WIDGET".equals(intent.getAction())) {
                    loadNotes();
                    updateAppWidget();
                    if (isCalendarView) {
                        updateCalendarDisplay();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.kelo.noteapp.UPDATE_WIDGET");

        // Fix for Android 13+ (API 33+): specify export flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(noteUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(noteUpdateReceiver, filter);
        }
    }

    private void loadNotes() {
        notesList.clear();
        notesList.addAll(databaseHelper.getAllNotes());
        applySorting();
        noteAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void applySorting() {
        switch (currentSortMode) {
            case 1: // Date oldest first
                Collections.sort(notesList, new Comparator<Note>() {
                    @Override
                    public int compare(Note a, Note b) {
                        if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                        if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                        return Long.compare(a.getCreatedAt(), b.getCreatedAt());
                    }
                });
                break;
            case 2: // Category
                Collections.sort(notesList, new Comparator<Note>() {
                    @Override
                    public int compare(Note a, Note b) {
                        if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                        if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                        String catA = a.getCategory() != null ? a.getCategory() : "personal";
                        String catB = b.getCategory() != null ? b.getCategory() : "personal";
                        return catA.compareTo(catB);
                    }
                });
                break;
            case 3: // Title
                Collections.sort(notesList, new Comparator<Note>() {
                    @Override
                    public int compare(Note a, Note b) {
                        if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                        if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                        return a.getTitle().compareToIgnoreCase(b.getTitle());
                    }
                });
                break;
            default: // Date newest first (default)
                Collections.sort(notesList, new Comparator<Note>() {
                    @Override
                    public int compare(Note a, Note b) {
                        if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                        if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                        return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                    }
                });
                break;
        }
    }

    private void updateEmptyState() {
        if (notesList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        toggleViewMenuItem = menu.findItem(R.id.action_toggle_view);
        if (toggleViewMenuItem != null) {
            toggleViewMenuItem.setIcon(isCalendarView ? R.drawable.ic_list : R.drawable.ic_calendar);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        } else if (id == R.id.action_toggle_view) {
            toggleView(!isCalendarView);
            return true;
        } else if (id == R.id.action_sort) {
            showCustomSortDialog();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            currentSortMode = 0;
            saveSortMode();
            loadNotes();
            updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle);
        });

        cardDateOldest.setOnClickListener(v -> {
            currentSortMode = 1;
            saveSortMode();
            loadNotes();
            updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle);
        });

        cardCategory.setOnClickListener(v -> {
            currentSortMode = 2;
            saveSortMode();
            loadNotes();
            updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle);
        });

        cardTitle.setOnClickListener(v -> {
            currentSortMode = 3;
            saveSortMode();
            loadNotes();
            updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle);
        });

        dialog.show();
    }

    private void updateSortDialogSelection(ImageView checkDateNewest, ImageView checkDateOldest, ImageView checkCategory, ImageView checkTitle) {
        // Hide all check marks
        checkDateNewest.setVisibility(View.GONE);
        checkDateOldest.setVisibility(View.GONE);
        checkCategory.setVisibility(View.GONE);
        checkTitle.setVisibility(View.GONE);

        // Show current selection
        switch (currentSortMode) {
            case 0: checkDateNewest.setVisibility(View.VISIBLE); break;
            case 1: checkDateOldest.setVisibility(View.VISIBLE); break;
            case 2: checkCategory.setVisibility(View.VISIBLE); break;
            case 3: checkTitle.setVisibility(View.VISIBLE); break;
        }
    }

    private void saveSortMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(PREF_SORT_MODE, currentSortMode).apply();
    }

    @Override
    public void onNoteClick(int position) {
        List<Note> sourceList = isCalendarView && selectedDateAdapter != null ?
                selectedDateAdapter.notesList : notesList;

        if (position >= 0 && position < sourceList.size()) {
            Note note = sourceList.get(position);
            Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
            intent.putExtra("note_id", note.getId());
            intent.putExtra("note_title", note.getTitle());
            intent.putExtra("note_content", note.getContent());
            intent.putExtra("note_reminder", note.getReminderTime());
            startActivityForResult(intent, EDIT_NOTE_REQUEST);
        }
    }

    @Override
    public void onDeleteClick(int position) {
        List<Note> sourceList = isCalendarView && selectedDateAdapter != null ?
                selectedDateAdapter.notesList : notesList;
        NoteAdapter sourceAdapter = isCalendarView && selectedDateAdapter != null ?
                selectedDateAdapter : noteAdapter;

        if (position >= 0 && position < sourceList.size()) {
            Note note = sourceList.get(position);

            // Move to trash instead of permanent delete
            databaseHelper.moveToTrash(note.getId());
            sourceList.remove(position);
            sourceAdapter.notifyItemRemoved(position);

            // Show undo snackbar
            Snackbar.make(isCalendarView ? selectedDateNotes : recyclerView,
                            "Заметка перемещена в корзину", Snackbar.LENGTH_LONG)
                    .setAction("ОТМЕНИТЬ", v -> {
                        databaseHelper.restoreFromTrash(note.getId());
                        sourceList.add(position, note);
                        sourceAdapter.notifyItemInserted(position);
                        updateAppWidget();
                    })
                    .show();

            if (!isCalendarView) {
                updateEmptyState();
            } else {
                // Refresh calendar data
                updateCalendarDisplay();
                // Refresh selected date if applicable
                if (selectedDateAdapter != null && sourceList.isEmpty()) {
                    selectedDateNotes.setVisibility(View.GONE);
                    emptyDateView.setVisibility(View.VISIBLE);
                    emptyDateView.setText("Нет заметок на выбранную дату");
                }
            }
            updateAppWidget();
        }
    }

    @Override
    public void onCompleteClick(int position) {
        List<Note> sourceList = isCalendarView && selectedDateAdapter != null ?
                selectedDateAdapter.notesList : notesList;
        NoteAdapter sourceAdapter = isCalendarView && selectedDateAdapter != null ?
                selectedDateAdapter : noteAdapter;

        if (position >= 0 && position < sourceList.size()) {
            Note note = sourceList.get(position);
            note.setCompleted(!note.isCompleted());
            databaseHelper.updateNote(note);
            sourceAdapter.notifyItemChanged(position);
            updateAppWidget();

            // If completed, cancel any existing reminder
            if (note.isCompleted()) {
                cancelNotification(note.getId());
            } else {
                // If uncompleted and has reminder, reschedule it
                if (note.getReminderTime() > 0) {
                    scheduleNotification(note);
                }
            }
        }
    }

    @Override
    public void onPinClick(int position) {
        List<Note> sourceList = isCalendarView && selectedDateAdapter != null ?
                selectedDateAdapter.notesList : notesList;
        NoteAdapter sourceAdapter = isCalendarView && selectedDateAdapter != null ?
                selectedDateAdapter : noteAdapter;

        if (position >= 0 && position < sourceList.size()) {
            Note note = sourceList.get(position);
            note.setPinned(!note.isPinned());
            databaseHelper.updateNote(note);

            // Re-sort the list to move pinned notes to top
            if (!isCalendarView) {
                loadNotes(); // This will re-sort and refresh the main list
            } else {
                // For calendar view, re-sort the selected date notes
                Collections.sort(sourceList, new Comparator<Note>() {
                    @Override
                    public int compare(Note a, Note b) {
                        if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                        if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                        return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                    }
                });
                sourceAdapter.notifyDataSetChanged();
            }

            updateAppWidget();
        }
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("note_id")) {
            int noteId = intent.getIntExtra("note_id", -1);
            if (noteId != -1) {
                // Open specific note
                Note note = databaseHelper.getNote(noteId);
                if (note != null) {
                    Intent editIntent = new Intent(this, AddEditNoteActivity.class);
                    editIntent.putExtra("note_id", note.getId());
                    editIntent.putExtra("note_title", note.getTitle());
                    editIntent.putExtra("note_content", note.getContent());
                    editIntent.putExtra("note_reminder", note.getReminderTime());
                    startActivityForResult(editIntent, EDIT_NOTE_REQUEST);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (!isCalendarView) {
                loadNotes();
            } else {
                updateCalendarDisplay();
            }
            updateAppWidget();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isCalendarView) {
            loadNotes();
        } else {
            updateCalendarDisplay();
        }
        updateAppWidget();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noteUpdateReceiver != null) {
            unregisterReceiver(noteUpdateReceiver);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Напоминания заметок";
            String description = "Уведомления для запланированных заметок";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleNotification(Note note) {
        if (note.getReminderTime() <= System.currentTimeMillis()) {
            return; // Don't schedule past reminders
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, note.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, note.getReminderTime(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, note.getReminderTime(), pendingIntent);
        }
    }

    private void cancelNotification(int noteId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, noteId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    private void updateAppWidget() {
        Intent intent = new Intent(this, NotesWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName(this, NotesWidgetProvider.class);
        int[] ids = appWidgetManager.getAppWidgetIds(provider);

        if (ids.length > 0) {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);

            // Force update the widget
            for (int id : ids) {
                NotesWidgetProvider.updateAppWidget(this, appWidgetManager, id);
            }
        }
    }
}