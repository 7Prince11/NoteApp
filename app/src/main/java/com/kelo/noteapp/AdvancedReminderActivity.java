package com.kelo.noteapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdvancedReminderActivity extends AppCompatActivity {

    // Views
    private MaterialButton btnSelectDate, btnSelectTime, btnAddExtraReminder, btnSaveReminders, btnCancel;
    private TextView textSelectedDateTime, textNoExtraReminders;
    private SwitchMaterial switchRepeat;
    private LinearLayout repeatDaysContainer, extraRemindersContainer;
    private CheckBox checkMonday, checkTuesday, checkWednesday, checkThursday,
            checkFriday, checkSaturday, checkSunday;

    // Data
    private Calendar selectedDate;
    private Calendar selectedTime;
    private boolean hasSelectedDateTime = false;
    private List<ExtraReminder> extraReminders;
    private int repeatDays = 0;

    // Settings
    private static final String PREFS_NAME = "NotesAppPrefs";
    private static final String KEY_TIME_24H = "time_24h";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_reminder);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        setupListeners();

        selectedDate = Calendar.getInstance();
        selectedTime = Calendar.getInstance();
        extraReminders = new ArrayList<>();
    }

    private void initializeViews() {
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnAddExtraReminder = findViewById(R.id.btnAddExtraReminder);
        btnSaveReminders = findViewById(R.id.btnSaveReminders);
        btnCancel = findViewById(R.id.btnCancel);

        textSelectedDateTime = findViewById(R.id.textSelectedDateTime);
        textNoExtraReminders = findViewById(R.id.textNoExtraReminders);

        switchRepeat = findViewById(R.id.switchRepeat);
        repeatDaysContainer = findViewById(R.id.repeatDaysContainer);
        extraRemindersContainer = findViewById(R.id.extraRemindersContainer);

        checkMonday = findViewById(R.id.checkMonday);
        checkTuesday = findViewById(R.id.checkTuesday);
        checkWednesday = findViewById(R.id.checkWednesday);
        checkThursday = findViewById(R.id.checkThursday);
        checkFriday = findViewById(R.id.checkFriday);
        checkSaturday = findViewById(R.id.checkSaturday);
        checkSunday = findViewById(R.id.checkSunday);
    }

    private void setupListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnAddExtraReminder.setOnClickListener(v -> addExtraReminder());
        btnSaveReminders.setOnClickListener(v -> saveReminders());
        btnCancel.setOnClickListener(v -> finish());

        switchRepeat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            repeatDaysContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                clearRepeatDays();
            }
        });

        // Checkbox listeners for repeat days
        CheckBox[] checkBoxes = {checkMonday, checkTuesday, checkWednesday, checkThursday,
                checkFriday, checkSaturday, checkSunday};
        for (int i = 0; i < checkBoxes.length; i++) {
            final int dayIndex = i;
            checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateRepeatDays(dayIndex, isChecked);
            });
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeDisplay();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minute) -> {
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);
                    selectedTime.set(Calendar.SECOND, 0);
                    hasSelectedDateTime = true;
                    updateDateTimeDisplay();
                },
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE),
                use24HourFormat()
        );
        timePickerDialog.show();
    }

    private void updateDateTimeDisplay() {
        if (hasSelectedDateTime) {
            // Combine date and time
            Calendar combined = Calendar.getInstance();
            combined.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
            combined.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
            combined.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
            combined.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
            combined.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));
            combined.set(Calendar.SECOND, 0);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("ru"));
            SimpleDateFormat timeFormat = use24HourFormat() ?
                    new SimpleDateFormat("HH:mm", Locale.getDefault()) :
                    new SimpleDateFormat("hh:mm a", Locale.getDefault());

            String dateStr = dateFormat.format(combined.getTime());
            String timeStr = timeFormat.format(combined.getTime());

            textSelectedDateTime.setText(dateStr + " в " + timeStr);
            textSelectedDateTime.setVisibility(View.VISIBLE);
        } else {
            textSelectedDateTime.setVisibility(View.GONE);
        }
    }

    private void addExtraReminder() {
        // Create dialog for extra reminder
        ExtraReminderDialog dialog = new ExtraReminderDialog(this, (date, time) -> {
            ExtraReminder reminder = new ExtraReminder(date, time);
            extraReminders.add(reminder);
            updateExtraRemindersDisplay();
        });
        dialog.show();
    }

    private void updateExtraRemindersDisplay() {
        extraRemindersContainer.removeAllViews();

        if (extraReminders.isEmpty()) {
            extraRemindersContainer.addView(textNoExtraReminders);
        } else {
            for (int i = 0; i < extraReminders.size(); i++) {
                final int index = i;
                ExtraReminder reminder = extraReminders.get(i);

                View reminderView = createExtraReminderView(reminder, () -> {
                    extraReminders.remove(index);
                    updateExtraRemindersDisplay();
                });

                extraRemindersContainer.addView(reminderView);
            }
        }
    }

    private View createExtraReminderView(ExtraReminder reminder, Runnable onDelete) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_extra_reminder, extraRemindersContainer, false);

        TextView textReminderInfo = view.findViewById(R.id.textReminderInfo);
        ImageButton btnDelete = view.findViewById(R.id.btnDeleteReminder);

        // Format reminder info
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("ru"));
        SimpleDateFormat timeFormat = use24HourFormat() ?
                new SimpleDateFormat("HH:mm", Locale.getDefault()) :
                new SimpleDateFormat("hh:mm a", Locale.getDefault());

        String dateStr = dateFormat.format(new Date(reminder.getDateTime()));
        String timeStr = timeFormat.format(new Date(reminder.getDateTime()));
        textReminderInfo.setText(dateStr + " в " + timeStr);

        btnDelete.setOnClickListener(v -> onDelete.run());

        return view;
    }

    private void updateRepeatDays(int dayIndex, boolean isChecked) {
        if (isChecked) {
            repeatDays |= (1 << dayIndex);
        } else {
            repeatDays &= ~(1 << dayIndex);
        }
    }

    private void clearRepeatDays() {
        repeatDays = 0;
        checkMonday.setChecked(false);
        checkTuesday.setChecked(false);
        checkWednesday.setChecked(false);
        checkThursday.setChecked(false);
        checkFriday.setChecked(false);
        checkSaturday.setChecked(false);
        checkSunday.setChecked(false);
    }

    private void saveReminders() {
        // Validate main reminder
        if (!hasSelectedDateTime) {
            Toast.makeText(this, "Выберите дату и время для основного напоминания", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create main reminder datetime
        Calendar mainReminder = Calendar.getInstance();
        mainReminder.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
        mainReminder.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
        mainReminder.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
        mainReminder.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
        mainReminder.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));
        mainReminder.set(Calendar.SECOND, 0);

        // Check if main reminder is in the future
        if (mainReminder.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "Выберите время в будущем", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare result data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("reminder_time", mainReminder.getTimeInMillis());
        resultIntent.putExtra("repeat_days", repeatDays);

        // Add extra reminders (for future implementation)
        ArrayList<Long> extraReminderTimes = new ArrayList<>();
        for (ExtraReminder reminder : extraReminders) {
            if (reminder.getDateTime() > System.currentTimeMillis()) {
                extraReminderTimes.add(reminder.getDateTime());
            }
        }
        resultIntent.putExtra("extra_reminders", extraReminderTimes);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private boolean use24HourFormat() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_TIME_24H, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Inner class for extra reminders
    private static class ExtraReminder {
        private long dateTime;

        public ExtraReminder(Calendar date, Calendar time) {
            Calendar combined = Calendar.getInstance();
            combined.set(Calendar.YEAR, date.get(Calendar.YEAR));
            combined.set(Calendar.MONTH, date.get(Calendar.MONTH));
            combined.set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH));
            combined.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
            combined.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
            combined.set(Calendar.SECOND, 0);
            this.dateTime = combined.getTimeInMillis();
        }

        public long getDateTime() {
            return dateTime;
        }
    }
}