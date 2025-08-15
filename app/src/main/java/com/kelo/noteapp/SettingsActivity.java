package com.kelo.noteapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "NotesAppPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_NOTIFICATION_VIBRATE = "notification_vibrate";
    private static final String KEY_DEFAULT_REMINDER = "default_reminder";
    private static final String KEY_SORT_ORDER = "sort_order";
    private static final String KEY_TIME_24H = "time_24h"; // NEW

    private SharedPreferences preferences;

    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchSound;
    private SwitchMaterial switchVibrate;
    private SwitchMaterial switchTime24h;    // NEW

    private TextView textSortOrder;
    private TextView textDefaultReminder;
    private TextView textNotesCount;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(this);

        // Views
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchSound = findViewById(R.id.switchSound);
        switchVibrate = findViewById(R.id.switchVibrate);
        switchTime24h = findViewById(R.id.switchTime24h); // NEW

        textSortOrder = findViewById(R.id.textSortOrder);
        textDefaultReminder = findViewById(R.id.textDefaultReminder);
        textNotesCount = findViewById(R.id.textNotesCount);

        // Load current settings
        loadSettings();

        // Dark mode
        switchDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
                AppCompatDelegate.setDefaultNightMode(isChecked
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
                recreate();
            }
        });

        // Sound
        switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_NOTIFICATION_SOUND, isChecked).apply();
            }
        });

        // Vibrate
        switchVibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_NOTIFICATION_VIBRATE, isChecked).apply();
            }
        });

        // NEW: Time format 24h / 12h
        if (switchTime24h != null) {
            switchTime24h.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    preferences.edit().putBoolean(KEY_TIME_24H, isChecked).apply();
                    Toast.makeText(SettingsActivity.this,
                            isChecked ? "Формат времени: 24 часа" : "Формат времени: 12 часов (AM/PM)",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Sort order selector
        View layoutSortOrder = findViewById(R.id.layoutSortOrder);
        if (layoutSortOrder != null) {
            layoutSortOrder.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showSortOrderDialog();
                }
            });
        }

        // Default reminder selector
        View layoutDefaultReminder = findViewById(R.id.layoutDefaultReminder);
        if (layoutDefaultReminder != null) {
            layoutDefaultReminder.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showDefaultReminderDialog();
                }
            });
        }

        // Export
        View layoutExport = findViewById(R.id.layoutExport);
        if (layoutExport != null) {
            layoutExport.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    exportNotes();
                }
            });
        }

        // Import
        View layoutImport = findViewById(R.id.layoutImport);
        if (layoutImport != null) {
            layoutImport.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    importNotes();
                }
            });
        }

        // Clear all
        View layoutClearAll = findViewById(R.id.layoutClearAll);
        if (layoutClearAll != null) {
            layoutClearAll.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showClearAllDialog();
                }
            });
        }

        // About
        View layoutAbout = findViewById(R.id.layoutAbout);
        if (layoutAbout != null) {
            layoutAbout.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showAboutDialog();
                }
            });
        }

        // Notes count
        updateNotesCount();
    }

    private void loadSettings() {
        boolean darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        boolean sound = preferences.getBoolean(KEY_NOTIFICATION_SOUND, true);
        boolean vibrate = preferences.getBoolean(KEY_NOTIFICATION_VIBRATE, true);
        String sortOrder = preferences.getString(KEY_SORT_ORDER, "date_desc");
        int defaultReminder = preferences.getInt(KEY_DEFAULT_REMINDER, 0);
        boolean time24h = preferences.getBoolean(KEY_TIME_24H, true); // NEW default 24h

        switchDarkMode.setChecked(darkMode);
        switchSound.setChecked(sound);
        switchVibrate.setChecked(vibrate);
        if (switchTime24h != null) switchTime24h.setChecked(time24h);

        // Sort label
        if (textSortOrder != null) {
            switch (sortOrder) {
                case "date_asc": textSortOrder.setText("По дате (старые сверху)"); break;
                case "title":    textSortOrder.setText("По заголовку"); break;
                default:         textSortOrder.setText("По дате (новые сверху)");
            }
        }

        // Default reminder label
        if (textDefaultReminder != null) {
            textDefaultReminder.setText(defaultReminder == 0 ? "Не установлено" : defaultReminder + " мин");
        }
    }

    private void showSortOrderDialog() {
        final String[] options = {"По дате (новые сверху)", "По дате (старые сверху)", "По заголовку"};
        String currentSort = preferences.getString(KEY_SORT_ORDER, "date_desc");
        int checkedItem = 0;
        if ("date_asc".equals(currentSort)) checkedItem = 1;
        else if ("title".equals(currentSort)) checkedItem = 2;

        new AlertDialog.Builder(this)
                .setTitle("Сортировка заметок")
                .setSingleChoiceItems(options, checkedItem, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String sortOrder = which == 1 ? "date_asc" : which == 2 ? "title" : "date_desc";
                        preferences.edit().putString(KEY_SORT_ORDER, sortOrder).apply();
                        if (textSortOrder != null) textSortOrder.setText(options[which]);
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showDefaultReminderDialog() {
        String[] options = {"Не установлено", "5 минут", "10 минут", "15 минут", "30 минут", "1 час"};
        int[] values = {0, 5, 10, 15, 30, 60};
        int currentValue = preferences.getInt(KEY_DEFAULT_REMINDER, 0);
        int checkedItem = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentValue) { checkedItem = i; break; }
        }

        new AlertDialog.Builder(this)
                .setTitle("Напоминание по умолчанию")
                .setSingleChoiceItems(options, checkedItem, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().putInt(KEY_DEFAULT_REMINDER, values[which]).apply();
                        if (textDefaultReminder != null) {
                            textDefaultReminder.setText(options[which].replace(" минут", " мин"));
                        }
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void exportNotes() {
        Toast.makeText(this, "Функция экспорта в разработке", Toast.LENGTH_SHORT).show();
    }

    private void importNotes() {
        Toast.makeText(this, "Функция импорта в разработке", Toast.LENGTH_SHORT).show();
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить все заметки?")
                .setMessage("Это действие удалит все заметки безвозвратно. Продолжить?")
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        databaseHelper.deleteAllNotes();
                        updateNotesCount();
                        Toast.makeText(SettingsActivity.this, "Все заметки удалены", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("О приложении")
                .setMessage("MindStack v1.0\n\nПростое и удобное приложение для создания заметок с напоминаниями.\n\n© 2024 Example Company")
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateNotesCount() {
        int count = databaseHelper.getNotesCount();
        if (textNotesCount != null) {
            textNotesCount.setText("Всего заметок: " + count);
        }
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
