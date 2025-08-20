// app/src/main/java/com/kelo/noteapp/AddEditNoteActivity.java
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditNoteActivity extends AppCompatActivity {

    private EditText editTitle, editContent;
    private Chip chipReminder;
    private TextView textReminderDateTime;
    private TextView textRepeatDays;
    private ImageButton btnClearReminder;
    private Spinner spinnerCategory;

    private View reminderDetailsContainer;

    private ChipGroup chipGroupCategory;
    private Chip chipPersonal, chipWork, chipFamily, chipErrand, chipOther, chipEveryday;

    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnSave;

    private DatabaseHelper databaseHelper;
    private int noteId = -1;
    private long reminderTime = 0;
    private Calendar reminderCalendar;
    private int repeatDays = 0;

    private boolean originalIsPinned = false;
    private boolean originalIsCompleted = false;

    private static final String PREFS_NAME = "NotesAppPrefs";
    private static final String KEY_TIME_24H = "time_24h";

    private static final String[] CAT_KEYS =    {"work","personal","family","errand","other","everyday"};
    private static final String[] CAT_DISPLAY = {"Работа","Личное","Семья","Поручение","Другое","Ежедневно"};

    private static final int ADVANCED_REMINDER_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        chipReminder = findViewById(R.id.chipReminder);
        textReminderDateTime = findViewById(R.id.textReminderDateTime);
        textRepeatDays = findViewById(R.id.textRepeatDays);
        btnClearReminder = findViewById(R.id.btnClearReminder);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        reminderDetailsContainer = findViewById(R.id.reminderDetailsContainer);

        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        chipPersonal = findViewById(R.id.chipPersonal);
        chipWork = findViewById(R.id.chipWork);
        chipFamily = findViewById(R.id.chipFamily);
        chipErrand = findViewById(R.id.chipErrand);
        chipOther = findViewById(R.id.chipOther);
        chipEveryday = findViewById(R.id.chipEveryday);

        btnSave = findViewById(R.id.btnSave);

        databaseHelper = new DatabaseHelper(this);
        reminderCalendar = Calendar.getInstance();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CAT_DISPLAY);
        spinnerCategory.setAdapter(adapter);

        Intent intent = getIntent();
        if (intent.hasExtra("note_id")) {
            noteId = intent.getIntExtra("note_id", -1);
            Note existing = databaseHelper.getNote(noteId);
            if (existing != null) {
                editTitle.setText(existing.getTitle());
                editContent.setText(existing.getContent());
                reminderTime = existing.getReminderTime();
                repeatDays = existing.getRepeatDays();

                originalIsPinned = existing.isPinned();
                originalIsCompleted = existing.isCompleted();

                int idx = indexOfKey(existing.getCategory());
                spinnerCategory.setSelection(idx >= 0 ? idx : 1);
                setCategoryChip(existing.getCategory());

                if (reminderTime > 0) {
                    reminderCalendar.setTimeInMillis(reminderTime);
                    updateReminderDisplay();
                }
            }
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Редактировать заметку");
        } else {
            spinnerCategory.setSelection(1);
            setCategoryChip("personal");
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Новая заметка");
        }

        chipReminder.setOnClickListener(v -> openAdvancedReminderInterface());
        btnClearReminder.setOnClickListener(v -> clearReminder());
        btnSave.setOnClickListener(v -> saveNote());
    }

    private void openAdvancedReminderInterface() {
        Intent intent = new Intent(this, AdvancedReminderActivity.class);
        if (reminderTime > 0) {
            intent.putExtra("existing_reminder_time", reminderTime);
            intent.putExtra("existing_repeat_days", repeatDays);
        }
        startActivityForResult(intent, ADVANCED_REMINDER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADVANCED_REMINDER_REQUEST && resultCode == RESULT_OK && data != null) {
            reminderTime = data.getLongExtra("reminder_time", 0);
            repeatDays = data.getIntExtra("repeat_days", 0);

            ArrayList<Long> extraReminders = (ArrayList<Long>) data.getSerializableExtra("extra_reminders");

            if (reminderTime > 0) {
                reminderCalendar.setTimeInMillis(reminderTime);
                updateReminderDisplay();
            }

            if (extraReminders != null && !extraReminders.isEmpty()) {
                Toast.makeText(this, "Дополнительные напоминания: " + extraReminders.size(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setCategoryChip(String categoryKey) {
        chipGroupCategory.clearCheck();

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
            case "everyday":
                chipEveryday.setChecked(true);
                break;
            case "personal":
            default:
                chipPersonal.setChecked(true);
                break;
        }
    }

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
        } else if (checkedChipId == R.id.chipEveryday) {
            return "everyday";
        } else {
            return "personal";
        }
    }

    private int indexOfKey(String key) {
        if (key == null) return -1;
        for (int i = 0; i < CAT_KEYS.length; i++) if (key.equals(CAT_KEYS[i])) return i;
        return -1;
    }

    private String selectedCategoryKey() {
        if (chipGroupCategory != null) {
            return getSelectedCategoryFromChips();
        } else {
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
                    updateReminderDisplay();
                },
                reminderCalendar.get(Calendar.HOUR_OF_DAY),
                reminderCalendar.get(Calendar.MINUTE),
                use24HourFormat()
        );
        timePickerDialog.show();
    }

    private void updateReminderDisplay() {
        if (reminderTime > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("ru"));
            SimpleDateFormat timeFormat = use24HourFormat() ?
                    new SimpleDateFormat("HH:mm", Locale.getDefault()) :
                    new SimpleDateFormat("hh:mm a", Locale.getDefault());

            String dateStr = dateFormat.format(new Date(reminderTime));
            String timeStr = timeFormat.format(new Date(reminderTime));

            String displayText = dateStr + " в " + timeStr;
            textReminderDateTime.setText(displayText);

            if (repeatDays > 0) {
                textRepeatDays.setText(getRepeatDaysText(repeatDays));
                textRepeatDays.setVisibility(View.VISIBLE);
            } else {
                textRepeatDays.setVisibility(View.GONE);
            }

            reminderDetailsContainer.setVisibility(View.VISIBLE);
            textReminderDateTime.setVisibility(View.VISIBLE);
            btnClearReminder.setVisibility(View.VISIBLE);
            chipReminder.setText("Изменить напоминание");
        } else {
            reminderDetailsContainer.setVisibility(View.GONE);
            textReminderDateTime.setVisibility(View.GONE);
            btnClearReminder.setVisibility(View.GONE);
            textRepeatDays.setVisibility(View.GONE);
            chipReminder.setText("Добавить напоминание");
        }
    }

    private String getRepeatDaysText(int repeatDays) {
        if (repeatDays == 0) return "";

        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 7; i++) {
            if ((repeatDays & (1 << i)) != 0) {
                if (result.length() > 0) result.append(", ");
                result.append(dayNames[i]);
            }
        }

        return "Повтор: " + result.toString();
    }

    private void clearReminder() {
        reminderTime = 0;
        repeatDays = 0;
        updateReminderDisplay();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty()) {
            editTitle.setError("Введите заголовок");
            editTitle.requestFocus();
            return;
        }

        String selectedCategory = selectedCategoryKey();

        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setCategory(selectedCategory);
        note.setReminderTime(reminderTime);
        note.setRepeatDays(repeatDays);

        if (noteId == -1) {
            note.setCreatedAt(System.currentTimeMillis());
            note.setCompleted(false);
            note.setPinned(false);
            long id = databaseHelper.addNote(note);
            note.setId((int) id);

            if (reminderTime > 0) {
                scheduleNotification(note);
            }
        } else {
            note.setId(noteId);
            note.setCreatedAt(System.currentTimeMillis());

            note.setPinned(originalIsPinned);
            note.setCompleted(originalIsCompleted);

            databaseHelper.updateNote(note);

            cancelNotification(noteId);
            if (reminderTime > 0) {
                scheduleNotification(note);
            }
        }

        Intent updateIntent = new Intent("com.kelo.noteapp.NOTE_UPDATED");
        sendBroadcast(updateIntent);

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

    private static int todayIndex(Calendar cal) {
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        return (dow == Calendar.SUNDAY) ? 6 : (dow - 2);
    }
}
