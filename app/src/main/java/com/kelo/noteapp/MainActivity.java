package com.kelo.noteapp;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnDateClickListener {

    // Optional legacy in-activity lists (not used with ViewPager2 tabs; left null-safe)
    private RecyclerView recyclerViewMain;
    private RecyclerView recyclerViewSecondary;
    private TextView mainListCount, secondPriorityCount;
    private TextView emptyView;

    private NoteAdapter mainAdapter;
    private NoteAdapter secondaryAdapter;
    private final List<Note> mainNotes = new ArrayList<>();
    private final List<Note> secondaryNotes = new ArrayList<>();

    // Tabs + pager
    private TabLayout tabs;
    private ViewPager2 viewPager;
    private PrimaryNotesFragment primaryFragment;
    private SecondaryNotesFragment secondaryFragment;

    // Calendar view
    private View calendarViewContainer;
    private RecyclerView calendarGrid;
    private CalendarAdapter calendarAdapter;
    private ImageButton btnPrevMonth, btnNextMonth;
    private TextView textMonthYear, textSelectedDate;
    private RecyclerView selectedDateNotes;
    private TextView emptyDateView;
    public NoteAdapter selectedDateAdapter;

    // Containers
    private View listViewContainer;
    private View fabAdd;

    // Common
    private DatabaseHelper databaseHelper;
    private boolean isCalendarView = false;
    private Calendar currentCalendar;
    private MenuItem toggleViewMenuItem;

    private BroadcastReceiver noteUpdateReceiver;

    // Sorting
    private int currentSortMode = 0; // 0 newest, 1 oldest, 2 category, 3 title
    private static final String PREF_SORT_MODE = "sort_mode";

    private static final String CHANNEL_ID = "notes_reminder_channel";
    private static final int ADD_NOTE_REQUEST = 1;
    private static final int EDIT_NOTE_REQUEST = 2;
    private static final String PREFS_NAME = "NotesAppPrefs";
    private static final String PREF_VIEW_MODE = "view_mode";

    // Category key for secondary folder
    private static final String SECONDARY_CATEGORY = "secondary";

    // Prevent swipe visuals while context menu is open
    private boolean suppressSwipe = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Init views
        initializeViews();

        databaseHelper = new DatabaseHelper(this);
        createNotificationChannel();

        setupTabsAndPager();      // tabs UI (primary / secondary)
        setupListView();          // legacy RVs (no-op if not present)
        setupCalendarView();      // calendar mode

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isCalendarView = prefs.getBoolean(PREF_VIEW_MODE, false);
        currentSortMode = prefs.getInt(PREF_SORT_MODE, 0);
        toggleView(isCalendarView);

        setupBroadcastReceiver();

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
                startActivityForResult(intent, ADD_NOTE_REQUEST);
            });
        }

        // Long-press actions for calendar's selected date list (fragments handle their own)


        handleNotificationIntent(getIntent());
    }

    private void initializeViews() {
        // Containers
        listViewContainer = findViewById(R.id.listViewContainer);
        calendarViewContainer = findViewById(R.id.calendarViewContainer);

        // Tabs & pager
        tabs = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.viewPager);

        // Optional legacy list RVs (not present in your new XML; keep null-safe)
        recyclerViewMain = findViewById(getIdByName("recyclerView"));
        recyclerViewSecondary = findViewById(getIdByName("recyclerViewSecondPriority"));
        mainListCount = findViewById(getIdByName("mainListCount"));
        secondPriorityCount = findViewById(getIdByName("secondPriorityCount"));
        emptyView = findViewById(getIdByName("emptyView"));

        // Calendar
        calendarGrid = findViewById(R.id.calendarGrid);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        textMonthYear = findViewById(R.id.textMonthYear);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        selectedDateNotes = findViewById(R.id.selectedDateNotes);
        emptyDateView = findViewById(R.id.emptyDateView);

        fabAdd = findViewById(R.id.fabAdd);
        currentCalendar = Calendar.getInstance();
    }

    private int getIdByName(String idName) {
        int id = getResources().getIdentifier(idName, "id", getPackageName());
        return id == 0 ? View.NO_ID : id;
    }

    // ---------------------------
    // Tabs + ViewPager2
    // ---------------------------
    private void setupTabsAndPager() {
        if (tabs == null || viewPager == null) return;

        primaryFragment = PrimaryNotesFragment.newInstance();
        secondaryFragment = SecondaryNotesFragment.newInstance();

        List<Fragment> frags = new ArrayList<>();
        frags.add(primaryFragment);
        frags.add(secondaryFragment);

        viewPager.setAdapter(new TabPagerAdapter(this, frags));
        new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("Основная");
            else tab.setText("Доп. папка");
        }).attach();

        viewPager.setOffscreenPageLimit(1);
    }

    private static class TabPagerAdapter extends FragmentStateAdapter {
        private final List<Fragment> fragments;

        public TabPagerAdapter(@NonNull FragmentActivity fa, @NonNull List<Fragment> fragments) {
            super(fa);
            this.fragments = fragments;
        }

        @NonNull @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }

    // ---------------------------
    // Legacy in-activity list mode (kept null-safe; your layout uses tabs)
    // ---------------------------
    private void setupListView() {
        if (recyclerViewMain == null) return;

        recyclerViewMain.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMain.setHasFixedSize(true);

        if (recyclerViewSecondary != null) {
            recyclerViewSecondary.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewSecondary.setHasFixedSize(true);
        }

        mainAdapter = new NoteAdapter(this, mainNotes, new NoteAdapter.OnNoteListener() {
            @Override public void onNoteClick(int position) { openForEdit(mainNotes, position); }
            @Override public void onDeleteClick(int position) { deleteFromList(mainNotes, mainAdapter, position); }
            @Override public void onCompleteClick(int position) { toggleComplete(mainNotes, mainAdapter, position); }
            @Override public void onPinClick(int position) { togglePin(mainNotes, mainAdapter, position); }
            // No onMoveToSecondary override here; move is via popup menu
        });

        secondaryAdapter = new NoteAdapter(this, secondaryNotes, new NoteAdapter.OnNoteListener() {
            @Override public void onNoteClick(int position) { openForEdit(secondaryNotes, position); }
            @Override public void onDeleteClick(int position) { deleteFromList(secondaryNotes, secondaryAdapter, position); }
            @Override public void onCompleteClick(int position) { toggleComplete(secondaryNotes, secondaryAdapter, position); }
            @Override public void onPinClick(int position) { togglePin(secondaryNotes, secondaryAdapter, position); }
            // No onMoveToSecondary override here; move is via popup menu
        });

        recyclerViewMain.setAdapter(mainAdapter);
        if (recyclerViewSecondary != null) recyclerViewSecondary.setAdapter(secondaryAdapter);

        setupSwipeToDelete(recyclerViewMain, mainNotes, mainAdapter);
        if (recyclerViewSecondary != null) setupSwipeToDelete(recyclerViewSecondary, secondaryNotes, secondaryAdapter);

        loadNotes();
    }

    // ---------------------------
    // Calendar
    // ---------------------------
    private void setupCalendarView() {
        calendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        calendarAdapter = new CalendarAdapter(this, this);
        calendarGrid.setAdapter(calendarAdapter);

        selectedDateNotes.setLayoutManager(new LinearLayoutManager(this));

        btnPrevMonth.setOnClickListener(v -> { currentCalendar.add(Calendar.MONTH, -1); updateCalendarDisplay(); });
        btnNextMonth.setOnClickListener(v -> { currentCalendar.add(Calendar.MONTH, 1); updateCalendarDisplay(); });

        updateCalendarDisplay();
    }

    private void updateCalendarDisplay() {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("ru"));
        textMonthYear.setText(monthFormat.format(currentCalendar.getTime()));

        Map<String, Integer> notesCountMap = databaseHelper.getNotesCountForMonth(year, month);
        Set<String> recurringDates = databaseHelper.getRecurringDatesForMonth(year, month);
        calendarAdapter.setMonth(year, month, notesCountMap, recurringDates);

        textSelectedDate.setVisibility(View.GONE);
        selectedDateNotes.setVisibility(View.GONE);
        emptyDateView.setVisibility(View.GONE);
    }

    @Override
    public void onDateClick(int year, int month, int day) {
        Calendar sel = Calendar.getInstance();
        sel.set(year, month, day);
        SimpleDateFormat df = new SimpleDateFormat("d MMMM yyyy", new Locale("ru"));
        textSelectedDate.setText("Заметки за " + df.format(sel.getTime()));
        textSelectedDate.setVisibility(View.VISIBLE);

        List<Note> dateNotes = databaseHelper.getNotesForDate(year, month, day);

        if (dateNotes.isEmpty()) {
            selectedDateNotes.setVisibility(View.GONE);
            emptyDateView.setVisibility(View.VISIBLE);
            emptyDateView.setText("Нет заметок на выбранную дату");
        } else {
            emptyDateView.setVisibility(View.GONE);
            selectedDateNotes.setVisibility(View.VISIBLE);
            selectedDateAdapter = new NoteAdapter(this, dateNotes, new NoteAdapter.OnNoteListener() {
                @Override public void onNoteClick(int position) { openForEdit(dateNotes, position); }
                @Override public void onDeleteClick(int position) { deleteFromList(dateNotes, selectedDateAdapter, position); }
                @Override public void onCompleteClick(int position) { toggleComplete(dateNotes, selectedDateAdapter, position); }
                @Override public void onPinClick(int position) { togglePin(dateNotes, selectedDateAdapter, position); }
                // Move handled via popup menu
            });
            selectedDateNotes.setAdapter(selectedDateAdapter);

            setupSwipeToDelete(selectedDateNotes, dateNotes, selectedDateAdapter);
        }
    }

    // ---------------------------
    // View toggle
    // ---------------------------
    private void toggleView(boolean showCalendar) {
        isCalendarView = showCalendar;

        if (showCalendar) {
            listViewContainer.setVisibility(View.GONE);
            calendarViewContainer.setVisibility(View.VISIBLE);
            updateCalendarDisplay();
            if (toggleViewMenuItem != null) toggleViewMenuItem.setIcon(R.drawable.ic_list);
        } else {
            calendarViewContainer.setVisibility(View.GONE);
            listViewContainer.setVisibility(View.VISIBLE);
            notifyTabsChanged(); // ensure fragments refresh when returning to list
            loadNotes();         // keeps counters/legacy lists up to date (null-safe)
            if (toggleViewMenuItem != null) toggleViewMenuItem.setIcon(R.drawable.ic_calendar);
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_VIEW_MODE, showCalendar).apply();
    }

    // ---------------------------
    // Swipe-to-delete with suppression when context menu is open
    // ---------------------------
    private void setupSwipeToDelete(RecyclerView rv, List<Note> sourceList, NoteAdapter adapter) {
        if (rv == null) return;

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (suppressSwipe) return 0;
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.35f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 2f;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos < 0 || pos >= sourceList.size()) return;
                Note note = sourceList.get(pos);

                databaseHelper.moveToTrash(note.getId());
                sourceList.remove(pos);
                adapter.notifyItemRemoved(pos);
                updateCountsAndEmpty();
                notifyTabsChanged();

                Snackbar.make(rv, "Заметка перемещена в корзину", Snackbar.LENGTH_LONG)
                        .setAction("ОТМЕНИТЬ", v -> {
                            databaseHelper.restoreFromTrash(note.getId());
                            sourceList.add(pos, note);
                            adapter.notifyItemInserted(pos);
                            updateCountsAndEmpty();
                            notifyTabsChanged();
                            updateAppWidget();
                        })
                        .show();

                updateAppWidget();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (suppressSwipe) {
                    super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive);
                    return;
                }

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                    paint.setColor(ContextCompat.getColor(MainActivity.this, R.color.delete_color));

                    if (dX > 0) {
                        c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), paint);
                    } else {
                        c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), paint);
                    }

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
        };

        new ItemTouchHelper(callback).attachToRecyclerView(rv);
    }

    private void setupBroadcastReceiver() {
        noteUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.kelo.noteapp.UPDATE_WIDGET".equals(intent.getAction())) {
                    if (!isCalendarView) {
                        notifyTabsChanged();
                        loadNotes();
                    } else {
                        updateCalendarDisplay();
                    }
                    updateAppWidget();
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.kelo.noteapp.UPDATE_WIDGET");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(noteUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(noteUpdateReceiver, filter);
        }
    }

    // ---------------------------
    // Data load used for counters/legacy views; fragments do their own reload()
    // ---------------------------
    private void loadNotes() {
        if (databaseHelper == null) return;

        mainNotes.clear();
        secondaryNotes.clear();

        // Only active notes (not in trash)
        List<Note> all = databaseHelper.getActiveNotes();

        for (Note n : all) {
            String cat = n.getCategory();
            if (SECONDARY_CATEGORY.equalsIgnoreCase(cat)) secondaryNotes.add(n);
            else mainNotes.add(n);
        }

        applySortingToList(mainNotes);
        applySortingToList(secondaryNotes);

        if (recyclerViewMain != null) {
            if (recyclerViewMain.getLayoutManager() == null) {
                recyclerViewMain.setLayoutManager(new LinearLayoutManager(this));
            }
            if (recyclerViewMain.getAdapter() == null) {
                recyclerViewMain.setAdapter(mainAdapter);
            }
            if (mainAdapter != null) mainAdapter.notifyDataSetChanged();
        }

        if (recyclerViewSecondary != null) {
            if (recyclerViewSecondary.getLayoutManager() == null) {
                recyclerViewSecondary.setLayoutManager(new LinearLayoutManager(this));
            }
            if (recyclerViewSecondary.getAdapter() == null) {
                recyclerViewSecondary.setAdapter(secondaryAdapter);
            }
            if (secondaryAdapter != null) secondaryAdapter.notifyDataSetChanged();
        }

        updateCountsAndEmpty();
    }

    private void applySortingToList(List<Note> list) {
        switch (currentSortMode) {
            case 1:
                Collections.sort(list, (a, b) -> {
                    if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                    if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                    return Long.compare(a.getCreatedAt(), b.getCreatedAt());
                });
                break;
            case 2:
                Collections.sort(list, (a, b) -> {
                    if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                    if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                    String ca = a.getCategory() != null ? a.getCategory() : "personal";
                    String cb = b.getCategory() != null ? b.getCategory() : "personal";
                    return ca.compareTo(cb);
                });
                break;
            case 3:
                Collections.sort(list, (a, b) -> {
                    if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                    if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                    return a.getTitle().compareToIgnoreCase(b.getTitle());
                });
                break;
            default:
                Collections.sort(list, (a, b) -> {
                    if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                    if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                    return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                });
                break;
        }
    }

    private void updateCountsAndEmpty() {
        int mainCount = mainNotes.size();
        int secondaryCount = secondaryNotes.size();

        if (mainListCount != null) mainListCount.setText(String.valueOf(mainCount));
        if (secondPriorityCount != null) secondPriorityCount.setText(String.valueOf(secondaryCount));
        if (emptyView != null) {
            emptyView.setVisibility((mainCount == 0 && secondaryCount == 0) ? View.VISIBLE : View.GONE);
        }
    }

    // ---------------------------
    // Long-press popup (Pin / Move) with swipe suppression
    // ---------------------------


    private void showNoteActions(View anchor, Note note, boolean isSecondaryList, boolean isCalendarSelectedList) {
        PopupMenu menu = new PopupMenu(this, anchor);

        // Block swipe while menu is visible
        suppressSwipe = true;
        menu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu popupMenu) {
                suppressSwipe = false;
            }
        });

        menu.getMenu().add(0, 1, 0, note.isPinned() ? "Открепить" : "Закрепить");
        if (SECONDARY_CATEGORY.equals(note.getCategory())) {
            menu.getMenu().add(0, 2, 1, "Переместить в основную");
        } else {
            menu.getMenu().add(0, 3, 1, "Переместить в доп. папку");
        }

        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                boolean newPinned = !note.isPinned();
                databaseHelper.updateNotePinned(note.getId(), newPinned);
                note.setPinned(newPinned);
                if (isCalendarView && isCalendarSelectedList) {
                    if (selectedDateAdapter != null) {
                        applySortingToList(selectedDateAdapter.notesList);
                        selectedDateAdapter.notifyDataSetChanged();
                    }
                } else {
                    notifyTabsChanged();
                    loadNotes();
                }
                updateAppWidget();
                return true;
            } else if (item.getItemId() == 2) { // move to main
                databaseHelper.updateNoteCategory(note.getId(), "personal");
                notifyTabsChanged();
                loadNotes();
                updateAppWidget();
                return true;
            } else if (item.getItemId() == 3) { // move to secondary
                databaseHelper.updateNoteCategory(note.getId(), SECONDARY_CATEGORY);
                notifyTabsChanged();
                loadNotes();
                updateAppWidget();
                return true;
            }
            return false;
        });
        menu.show();
    }

    // ---------------------------
    // Adapter helpers
    // ---------------------------
    private void openForEdit(List<Note> source, int pos) {
        if (pos < 0 || pos >= source.size()) return;
        Note note = source.get(pos);
        Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());
        intent.putExtra("note_reminder", note.getReminderTime());
        startActivityForResult(intent, EDIT_NOTE_REQUEST);
    }

    public void openEditFromNote(Note note) {
        if (note == null) return;
        Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());
        intent.putExtra("note_reminder", note.getReminderTime());
        startActivityForResult(intent, EDIT_NOTE_REQUEST);
    }

    public void notifyTabsChanged() {
        if (primaryFragment != null && primaryFragment.isAdded()) {
            primaryFragment.reload();
        }
        if (secondaryFragment != null && secondaryFragment.isAdded()) {
            secondaryFragment.reload();
        }
        updateAppWidget();
    }

    public void handleCompleteToggle(Note note) {
        if (note == null) return;
        if (note.isCompleted()) cancelNotification(note.getId());
        else if (note.getReminderTime() > 0) scheduleNotification(note);
        notifyTabsChanged();
        updateAppWidget();
    }

    private void deleteFromList(List<Note> source, NoteAdapter adapter, int pos) {
        if (pos < 0 || pos >= source.size()) return;
        Note note = source.get(pos);
        databaseHelper.moveToTrash(note.getId());
        source.remove(pos);
        adapter.notifyItemRemoved(pos);
        updateCountsAndEmpty();
        notifyTabsChanged();
        Snackbar.make(findViewById(android.R.id.content), "Заметка перемещена в корзину", Snackbar.LENGTH_LONG)
                .setAction("ОТМЕНИТЬ", v -> {
                    databaseHelper.restoreFromTrash(note.getId());
                    source.add(pos, note);
                    adapter.notifyItemInserted(pos);
                    updateCountsAndEmpty();
                    notifyTabsChanged();
                    updateAppWidget();
                })
                .show();
        updateAppWidget();
    }

    private void toggleComplete(List<Note> source, NoteAdapter adapter, int pos) {
        if (pos < 0 || pos >= source.size()) return;
        Note note = source.get(pos);
        note.setCompleted(!note.isCompleted());
        databaseHelper.updateNote(note);
        adapter.notifyItemChanged(pos);

        if (note.isCompleted()) cancelNotification(note.getId());
        else if (note.getReminderTime() > 0) scheduleNotification(note);

        notifyTabsChanged();
        updateAppWidget();
    }

    private void togglePin(List<Note> source, NoteAdapter adapter, int pos) {
        if (pos < 0 || pos >= source.size()) return;
        Note note = source.get(pos);
        note.setPinned(!note.isPinned());
        databaseHelper.updateNote(note);
        applySortingToList(source);
        adapter.notifyDataSetChanged();
        notifyTabsChanged();
        updateAppWidget();
    }

    // ---------------------------
    // Menu
    // ---------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        toggleViewMenuItem = menu.findItem(R.id.action_toggle_view);
        if (toggleViewMenuItem != null) toggleViewMenuItem.setIcon(isCalendarView ? R.drawable.ic_list : R.drawable.ic_calendar);
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

    private void showCustomSortDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sort_notes);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        androidx.cardview.widget.CardView cardDateNewest = dialog.findViewById(R.id.cardDateNewest);
        androidx.cardview.widget.CardView cardDateOldest = dialog.findViewById(R.id.cardDateOldest);
        androidx.cardview.widget.CardView cardCategory = dialog.findViewById(R.id.cardCategory);
        androidx.cardview.widget.CardView cardTitle = dialog.findViewById(R.id.cardTitle);

        ImageView checkDateNewest = dialog.findViewById(R.id.checkDateNewest);
        ImageView checkDateOldest = dialog.findViewById(R.id.checkDateOldest);
        ImageView checkCategory = dialog.findViewById(R.id.checkCategory);
        ImageView checkTitle = dialog.findViewById(R.id.checkTitle);

        ImageButton btnCloseDialog = dialog.findViewById(R.id.btnCloseDialog);

        updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle);

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        cardDateNewest.setOnClickListener(v -> { currentSortMode = 0; saveSortMode(); notifyTabsChanged(); loadNotes(); updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle); });
        cardDateOldest.setOnClickListener(v -> { currentSortMode = 1; saveSortMode(); notifyTabsChanged(); loadNotes(); updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle); });
        cardCategory.setOnClickListener(v -> { currentSortMode = 2; saveSortMode(); notifyTabsChanged(); loadNotes(); updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle); });
        cardTitle.setOnClickListener(v -> { currentSortMode = 3; saveSortMode(); notifyTabsChanged(); loadNotes(); updateSortDialogSelection(checkDateNewest, checkDateOldest, checkCategory, checkTitle); });

        dialog.show();
    }

    private void updateSortDialogSelection(ImageView checkDateNewest, ImageView checkDateOldest,
                                           ImageView checkCategory, ImageView checkTitle) {
        checkDateNewest.setVisibility(View.GONE);
        checkDateOldest.setVisibility(View.GONE);
        checkCategory.setVisibility(View.GONE);
        checkTitle.setVisibility(View.GONE);
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

    // ---------------------------
    // Notifications / widget
    // ---------------------------
    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("note_id")) {
            int noteId = intent.getIntExtra("note_id", -1);
            if (noteId != -1) {
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
                notifyTabsChanged();
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
            notifyTabsChanged();
            loadNotes();
        } else {
            updateCalendarDisplay();
        }
        updateAppWidget();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noteUpdateReceiver != null) unregisterReceiver(noteUpdateReceiver);
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
        if (note.getReminderTime() <= System.currentTimeMillis()) return;

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
            for (int id : ids) {
                NotesWidgetProvider.updateAppWidget(this, appWidgetManager, id);
            }
        }
    }
}
