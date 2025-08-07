package com.kelo.noteapp;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditNoteActivity extends AppCompatActivity {

    private EditText editTitle, editContent;
    private Chip chipReminder;
    private TextView textReminderDateTime;
    private ImageButton btnClearReminder;
    private Button btnSave;
    private DatabaseHelper databaseHelper;

    private int noteId = -1;
    private long reminderTime = 0;
    private Calendar reminderCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        // Настройка toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Инициализация views
        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        chipReminder = findViewById(R.id.chipReminder);
        textReminderDateTime = findViewById(R.id.textReminderDateTime);
        btnClearReminder = findViewById(R.id.btnClearReminder);
        btnSave = findViewById(R.id.btnSave);

        databaseHelper = new DatabaseHelper(this);
        reminderCalendar = Calendar.getInstance();

        // Проверка режима (добавление или редактирование)
        Intent intent = getIntent();
        if (intent.hasExtra("note_id")) {
            noteId = intent.getIntExtra("note_id", -1);
            editTitle.setText(intent.getStringExtra("note_title"));
            editContent.setText(intent.getStringExtra("note_content"));
            reminderTime = intent.getLongExtra("note_reminder", 0);

            if (reminderTime > 0) {
                reminderCalendar.setTimeInMillis(reminderTime);
                updateReminderDisplay();
            }

            getSupportActionBar().setTitle("Редактировать заметку");
        } else {
            getSupportActionBar().setTitle("Новая заметка");
        }

        // Обработчик для установки напоминания
        chipReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateTimePicker();
            }
        });

        // Обработчик для очистки напоминания
        btnClearReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearReminder();
            }
        });

        // Обработчик сохранения
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });
    }

    private void showDateTimePicker() {
        // Сначала выбор даты
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        reminderCalendar.set(Calendar.YEAR, year);
                        reminderCalendar.set(Calendar.MONTH, month);
                        reminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        // Затем выбор времени
                        showTimePicker();
                    }
                },
                reminderCalendar.get(Calendar.YEAR),
                reminderCalendar.get(Calendar.MONTH),
                reminderCalendar.get(Calendar.DAY_OF_MONTH)
        );

        // Установка минимальной даты (сегодня)
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        reminderCalendar.set(Calendar.MINUTE, minute);
                        reminderCalendar.set(Calendar.SECOND, 0);

                        // Проверка что время не в прошлом
                        if (reminderCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
                            Toast.makeText(AddEditNoteActivity.this,
                                    "Выберите время в будущем", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        reminderTime = reminderCalendar.getTimeInMillis();
                        updateReminderDisplay();
                    }
                },
                reminderCalendar.get(Calendar.HOUR_OF_DAY),
                reminderCalendar.get(Calendar.MINUTE),
                true // 24-часовой формат
        );

        timePickerDialog.show();
    }

    private void updateReminderDisplay() {
        if (reminderTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("ru"));
            String dateTime = sdf.format(new Date(reminderTime));
            textReminderDateTime.setText("Напоминание: " + dateTime);
            textReminderDateTime.setVisibility(View.VISIBLE);
            btnClearReminder.setVisibility(View.VISIBLE);
            chipReminder.setText("Изменить напоминание");
        }
    }

    private void clearReminder() {
        reminderTime = 0;
        textReminderDateTime.setVisibility(View.GONE);
        btnClearReminder.setVisibility(View.GONE);
        chipReminder.setText("Добавить напоминание");
        Toast.makeText(this, "Напоминание удалено", Toast.LENGTH_SHORT).show();
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty()) {
            editTitle.setError("Введите заголовок");
            return;
        }

        if (content.isEmpty()) {
            editContent.setError("Введите содержание");
            return;
        }

        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setReminderTime(reminderTime);
        note.setCreatedAt(System.currentTimeMillis());

        if (noteId == -1) {
            // Добавление новой заметки
            long id = databaseHelper.addNote(note);
            note.setId((int) id);

            if (reminderTime > 0) {
                scheduleNotification(note);
            }

            Toast.makeText(this, "Заметка добавлена", Toast.LENGTH_SHORT).show();
        } else {
            // Обновление существующей заметки
            note.setId(noteId);
            databaseHelper.updateNote(note);

            // Переустановка уведомления
            cancelNotification(noteId);
            if (reminderTime > 0) {
                scheduleNotification(note);
            }

            Toast.makeText(this, "Заметка обновлена", Toast.LENGTH_SHORT).show();
        }

        setResult(RESULT_OK);
        finish();
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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
            );
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}