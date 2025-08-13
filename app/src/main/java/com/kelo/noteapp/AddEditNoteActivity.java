package com.kelo.noteapp;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditNoteActivity extends AppCompatActivity {

    private EditText editTitle, editContent;
    private Chip chipReminder;
    private TextView textReminderDateTime;
    private TextView textRepeatDays;
    private ImageButton btnClearReminder;
    private Spinner spinnerCategory; // Kept for compatibility

    // NEW: ChipGroup and individual chips for categories
    private ChipGroup chipGroupCategory;
    private Chip chipPersonal, chipWork, chipFamily, chipErrand, chipOther;

    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnSave;

    private DatabaseHelper databaseHelper;
    private int noteId = -1;
    private long reminderTime = 0;
    private Calendar reminderCalendar;
    private int repeatDays = 0;

    private static final String PREFS_NAME = "NotesAppPrefs";
    private static final String KEY_TIME_24H = "time_24h";

    // Categories: key -> display name (ru)
    private static final String[] CAT_KEYS =    {"work","personal","family","errand","other"};
    private static final String[] CAT_DISPLAY = {"Работа","Личное","Семья","Поручение","Другое"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Views
        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        chipReminder = findViewById(R.id.chipReminder);
        textReminderDateTime = findViewById(R.id.textReminderDateTime);
        textRepeatDays = findViewById(R.id.textRepeatDays);
        btnClearReminder = findViewById(R.id.btnClearReminder);
        spinnerCategory = findViewById(R.id.spinnerCategory); // Kept for compatibility

        // NEW: Initialize ChipGroup and individual chips
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        chipPersonal = findViewById(R.id.chipPersonal);
        chipWork = findViewById(R.id.chipWork);
        chipFamily = findViewById(R.id.chipFamily);
        chipErrand = findViewById(R.id.chipErrand);
        chipOther = findViewById(R.id.chipOther);

        btnSave = findViewById(R.id.btnSave);

        databaseHelper = new DatabaseHelper(this);
        reminderCalendar = Calendar.getInstance();

        // Category spinner setup (kept for compatibility)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CAT_DISPLAY);
        spinnerCategory.setAdapter(adapter);

        // Edit mode?
        Intent intent = getIntent();
        if (intent.hasExtra("note_id")) {
            noteId = intent.getIntExtra("note_id", -1);
            Note existing = databaseHelper.getNote(noteId);
            if (existing != null) {
                editTitle.setText(existing.getTitle());
                editContent.setText(existing.getContent());
                reminderTime = existing.getReminderTime();
                repeatDays = existing.getRepeatDays();

                // Set category using both methods for compatibility
                int idx = indexOfKey(existing.getCategory());
                spinnerCategory.setSelection(idx >= 0 ? idx : 1); // default "Личное"
                // NEW: Also set the chip selection
                setCategoryChip(existing.getCategory());

                if (reminderTime > 0) {
                    reminderCalendar.setTimeInMillis(reminderTime);
                    updateReminderDisplay();
                }
            }
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Редактировать заметку");
        } else {
            // default category "Личное"
            spinnerCategory.setSelection(1);
            // NEW: Also set default chip
            setCategoryChip("personal");
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Новая заметка");
        }

        chipReminder.setOnClickListener(v -> showDateTimePicker());
        btnClearReminder.setOnClickListener(v -> clearReminder());
        btnSave.setOnClickListener(v -> saveNote());
    }

    // NEW: Method to set category chip selection
    private void setCategoryChip(String categoryKey) {
        // Clear all selections first
        chipGroupCategory.clearCheck();

        // Set the correct chip as checked
        switch (categoryKey == null ? "personal" : categoryKey) {
            case "work":
                chipWork.setChecked(true);
                break;
            case "family":
                chipFamily.setChecked(true);
                break;
            case "errand":
                chipErrand.setChecked(true);
                break;
            case "other":
                chipOther.setChecked(true);
                break;
            case "personal":
            default:
                chipPersonal.setChecked(true);
                break;
        }
    }

    // NEW: Method to get selected category from chips
    private String getSelectedCategoryFromChips() {
        int checkedChipId = chipGroupCategory.getCheckedChipId();

        if (checkedChipId == R.id.chipWork) {
            return "work";
        } else if (checkedChipId == R.id.chipFamily) {
            return "family";
        } else if (checkedChipId == R.id.chipErrand) {
            return "errand";
        } else if (checkedChipId == R.id.chipOther) {
            return "other";
        } else {
            return "personal"; // default
        }
    }

    private int indexOfKey(String key) {
        if (key == null) return -1;
        for (int i = 0; i < CAT_KEYS.length; i++) if (key.equals(CAT_KEYS[i])) return i;
        return -1;
    }

    private String selectedCategoryKey() {
        // NEW: Use chips if available, fallback to spinner
        if (chipGroupCategory != null) {
            return getSelectedCategoryFromChips();
        } else {
            // Fallback to spinner method
            int pos = spinnerCategory.getSelectedItemPosition();
            if (pos < 0 || pos >= CAT_KEYS.length) return "personal";
            return CAT_KEYS[pos];
        }
    }

    private void showDateTimePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    reminderCalendar.set(Calendar.YEAR, year);
                    reminderCalendar.set(Calendar.MONTH, month);
                    reminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showTimePicker();
                },
                reminderCalendar.get(Calendar.YEAR),
                reminderCalendar.get(Calendar.MONTH),
                reminderCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minute) -> {
                    reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    reminderCalendar.set(Calendar.MINUTE, minute);
                    reminderCalendar.set(Calendar.SECOND, 0);

                    if (reminderCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
                        Toast.makeText(AddEditNoteActivity.this, "Выберите время в будущем", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reminderTime = reminderCalendar.getTimeInMillis();
                    showRepeatDaysDialog(); // ask for repeating days
                },
                reminderCalendar.get(Calendar.HOUR_OF_DAY),
                reminderCalendar.get(Calendar.MINUTE),
                use24HourFormat()
        );
        timePickerDialog.show();
    }

    private void showRepeatDaysDialog() {
        final String[] days = new String[]{"Пн","Вт","Ср","Чт","Пт","Сб","Вс"};
        final boolean[] checked = new boolean[7];
        for (int i = 0; i < 7; i++) checked[i] = ((repeatDays >> i) & 1) == 1;

        new AlertDialog.Builder(this)
                .setTitle("Повтор по дням")
                .setMultiChoiceItems(days, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Готово", (dialog, which) -> {
                    int mask = 0;
                    for (int i = 0; i < 7; i++) if (checked[i]) mask |= (1 << i);
                    repeatDays = mask;
                    updateReminderDisplay();
                })
                .setNegativeButton("Отмена", (d, w) -> updateReminderDisplay())
                .show();
    }

    private void updateReminderDisplay() {
        if (reminderTime > 0) {
            // Show the reminder details container
            findViewById(R.id.reminderDetailsContainer).setVisibility(View.VISIBLE);

            String pattern = use24HourFormat() ? "dd MMM yyyy, HH:mm" : "dd MMM yyyy, hh:mm a";
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, new Locale("ru"));
            String base = sdf.format(new Date(reminderTime));

            textReminderDateTime.setText("Напоминание: " + base);
            chipReminder.setText("Изменить напоминание");

            if (repeatDays != 0) {
                textRepeatDays.setText("Повтор: " + repeatSummary(repeatDays));
                textRepeatDays.setVisibility(View.VISIBLE);
            } else {
                textRepeatDays.setVisibility(View.GONE);
            }
        } else {
            // Hide the entire container when no reminder
            findViewById(R.id.reminderDetailsContainer).setVisibility(View.GONE);
        }
    }

    private void clearReminder() {
        reminderTime = 0;
        repeatDays = 0;

        // Hide the entire reminder details container
        findViewById(R.id.reminderDetailsContainer).setVisibility(View.GONE);

        // Reset chip text
        chipReminder.setText("Добавить напоминание");

        Toast.makeText(this, "Напоминание удалено", Toast.LENGTH_SHORT).show();
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty()) { editTitle.setError("Введите заголовок"); return; }
        if (content.isEmpty()) { editContent.setError("Введите содержание"); return; }

        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setCreatedAt(System.currentTimeMillis());
        note.setRepeatDays(repeatDays);
        note.setCategory(selectedCategoryKey()); // This now uses chips

        if (reminderTime > 0) {
            if (repeatDays != 0) {
                long next = computeNextOccurrenceFromNow(repeatDays,
                        reminderCalendar.get(Calendar.HOUR_OF_DAY),
                        reminderCalendar.get(Calendar.MINUTE));
                reminderTime = next;
            }
            note.setReminderTime(reminderTime);
        } else {
            note.setReminderTime(0);
        }

        if (noteId == -1) {
            long id = databaseHelper.addNote(note);
            note.setId((int) id);
            if (note.getReminderTime() > 0) scheduleNotification(note);
            Toast.makeText(this, "Заметка добавлена", Toast.LENGTH_SHORT).show();
        } else {
            note.setId(noteId);
            databaseHelper.updateNote(note);
            cancelNotification(noteId);
            if (note.getReminderTime() > 0) scheduleNotification(note);
            Toast.makeText(this, "Заметка обновлена", Toast.LENGTH_SHORT).show();
        }

        setResult(RESULT_OK);
        finish();
    }

    private boolean use24HourFormat() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_TIME_24H, true);
    }

    private void scheduleNotification(Note note) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());

        PendingIntent pi = PendingIntent.getBroadcast(
                this, note.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long when = note.getReminderTime();
        if (am != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
            }
        }
    }

    private void cancelNotification(int noteId) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, noteId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (am != null) am.cancel(pi);
    }

    // ==== Repeat helpers ====
    private static int todayIndex(Calendar cal) {
        int dow = cal.get(Calendar.DAY_OF_WEEK); // Sun=1..Sat=7
        return (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY); // Mon->0 ... Sun->6
    }

    private static boolean isBitSet(int mask, int idx) {
        return ((mask >> idx) & 1) == 1;
    }

    private static String repeatSummary(int mask) {
        String[] names = {"Пн","Вт","Ср","Чт","Пт","Сб","Вс"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (isBitSet(mask, i)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(names[i]);
            }
        }
        return sb.toString();
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
            if (((mask >> idx) & 1) != 1) continue;

            Calendar test = (Calendar) cand.clone();
            test.add(Calendar.DAY_OF_MONTH, offset);
            if (test.getTimeInMillis() > now.getTimeInMillis()) {
                return test.getTimeInMillis();
            }
        }
        return now.getTimeInMillis() + 60_000;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}