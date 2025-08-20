package com.kelo.noteapp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ExtraReminderDialog extends Dialog {

    public interface OnReminderSetListener {
        void onReminderSet(Calendar date, Calendar time);
    }

    private Context context;
    private OnReminderSetListener listener;

    private MaterialButton btnSelectDate, btnSelectTime;
    private TextView textSelectedDateTime;
    private MaterialButton btnSave, btnCancel;

    private Calendar selectedDate;
    private Calendar selectedTime;
    private boolean hasDate = false;
    private boolean hasTime = false;

    private static final String PREFS_NAME = "NotesAppPrefs";
    private static final String KEY_TIME_24H = "time_24h";

    public ExtraReminderDialog(@NonNull Context context, OnReminderSetListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;

        setupDialog();
    }

    private void setupDialog() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_extra_reminder, null);
        setContentView(view);

        initializeViews(view);
        setupListeners();

        selectedDate = Calendar.getInstance();
        selectedTime = Calendar.getInstance();

        setTitle("Дополнительное напоминание");
        setCancelable(true);

        // Set dialog size
        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9),
                    -2 // WRAP_CONTENT
            );
        }
    }

    private void initializeViews(View view) {
        btnSelectDate = view.findViewById(R.id.btnSelectDate);
        btnSelectTime = view.findViewById(R.id.btnSelectTime);
        textSelectedDateTime = view.findViewById(R.id.textSelectedDateTime);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);
    }

    private void setupListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnSave.setOnClickListener(v -> saveReminder());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    hasDate = true;
                    updateDisplay();
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
                context,
                (TimePicker view, int hourOfDay, int minute) -> {
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);
                    selectedTime.set(Calendar.SECOND, 0);
                    hasTime = true;
                    updateDisplay();
                },
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE),
                use24HourFormat()
        );
        timePickerDialog.show();
    }

    private void updateDisplay() {
        if (hasDate && hasTime) {
            // Combine date and time for display
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
        } else if (hasDate) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("ru"));
            textSelectedDateTime.setText("Дата: " + dateFormat.format(selectedDate.getTime()));
            textSelectedDateTime.setVisibility(View.VISIBLE);
        } else if (hasTime) {
            SimpleDateFormat timeFormat = use24HourFormat() ?
                    new SimpleDateFormat("HH:mm", Locale.getDefault()) :
                    new SimpleDateFormat("hh:mm a", Locale.getDefault());
            textSelectedDateTime.setText("Время: " + timeFormat.format(selectedTime.getTime()));
            textSelectedDateTime.setVisibility(View.VISIBLE);
        } else {
            textSelectedDateTime.setVisibility(View.GONE);
        }
    }

    private void saveReminder() {
        if (!hasDate || !hasTime) {
            Toast.makeText(context, "Выберите дату и время", Toast.LENGTH_SHORT).show();
            return;
        }

        // Combine date and time
        Calendar combined = Calendar.getInstance();
        combined.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
        combined.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
        combined.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
        combined.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
        combined.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));
        combined.set(Calendar.SECOND, 0);

        if (combined.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(context, "Выберите время в будущем", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listener != null) {
            listener.onReminderSet(selectedDate, selectedTime);
        }
        dismiss();
    }

    private boolean use24HourFormat() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_TIME_24H, true);
    }
}