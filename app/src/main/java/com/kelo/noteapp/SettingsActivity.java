package com.kelo.noteapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
    private static final String KEY_TIME_24H = "time_24h";
    private static final String KEY_TRASH_AUTO_DELETE_DAYS = "trash_auto_delete_days";

    private SharedPreferences preferences;

    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchSound;
    private SwitchMaterial switchVibrate;
    private SwitchMaterial switchTime24h;

    private TextView textSortOrder;
    private TextView textDefaultReminder;
    private TextView textNotesCount;
    private TextView textTrashAutoDelete;
    private TextView textTrashCount;

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
            getSupportActionBar().setTitle("Настройки");
        }

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(this);

        initViews();
        setupClickListeners();
        loadSettings();
    }

    private void initViews() {
        // Switches
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchSound = findViewById(R.id.switchSound);
        switchVibrate = findViewById(R.id.switchVibrate);
        switchTime24h = findViewById(R.id.switchTime24h);

        // TextViews
        textSortOrder = findViewById(R.id.textSortOrder);
        textDefaultReminder = findViewById(R.id.textDefaultReminder);
        textNotesCount = findViewById(R.id.textNotesCount);
        textTrashAutoDelete = findViewById(R.id.textTrashAutoDelete);
        textTrashCount = findViewById(R.id.textTrashCount);
    }

    private void setupClickListeners() {
        // Dark mode switch
        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    preferences.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
                    AppCompatDelegate.setDefaultNightMode(isChecked ?
                            AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                }
            });
        }

        // Sound switch
        if (switchSound != null) {
            switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    preferences.edit().putBoolean(KEY_NOTIFICATION_SOUND, isChecked).apply();
                }
            });
        }

        // Vibrate switch
        if (switchVibrate != null) {
            switchVibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    preferences.edit().putBoolean(KEY_NOTIFICATION_VIBRATE, isChecked).apply();
                }
            });
        }

        // 24h time switch
        if (switchTime24h != null) {
            switchTime24h.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    preferences.edit().putBoolean(KEY_TIME_24H, isChecked).apply();
                }
            });
        }

        // Sort order
        View layoutSortOrder = findViewById(R.id.layoutSortOrder);
        if (layoutSortOrder != null) {
            layoutSortOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSortOrderDialog();
                }
            });
        }

        // Default reminder
        View layoutDefaultReminder = findViewById(R.id.layoutDefaultReminder);
        if (layoutDefaultReminder != null) {
            layoutDefaultReminder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDefaultReminderDialog();
                }
            });
        }

        // Trash view
        View layoutTrash = findViewById(R.id.layoutTrash);
        if (layoutTrash != null) {
            layoutTrash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SettingsActivity.this, TrashActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Auto-delete trash
        View layoutTrashAutoDelete = findViewById(R.id.layoutTrashAutoDelete);
        if (layoutTrashAutoDelete != null) {
            layoutTrashAutoDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTrashAutoDeleteDialog();
                }
            });
        }

        // Empty trash
        View layoutEmptyTrash = findViewById(R.id.layoutEmptyTrash);
        if (layoutEmptyTrash != null) {
            layoutEmptyTrash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEmptyTrashDialog();
                }
            });
        }

        // Export
        View layoutExport = findViewById(R.id.layoutExport);
        if (layoutExport != null) {
            layoutExport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exportNotes();
                }
            });
        }

        // Import
        View layoutImport = findViewById(R.id.layoutImport);
        if (layoutImport != null) {
            layoutImport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    importNotes();
                }
            });
        }

        // Clear all
        View layoutClearAll = findViewById(R.id.layoutClearAll);
        if (layoutClearAll != null) {
            layoutClearAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showClearAllDialog();
                }
            });
        }

        // About
        View layoutAbout = findViewById(R.id.layoutAbout);
        if (layoutAbout != null) {
            layoutAbout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAboutDialog();
                }
            });
        }

        // Update counts
        updateCounts();
    }

    private void loadSettings() {
        boolean darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        boolean sound = preferences.getBoolean(KEY_NOTIFICATION_SOUND, true);
        boolean vibrate = preferences.getBoolean(KEY_NOTIFICATION_VIBRATE, true);
        String sortOrder = preferences.getString(KEY_SORT_ORDER, "date_desc");
        int defaultReminder = preferences.getInt(KEY_DEFAULT_REMINDER, 0);
        boolean time24h = preferences.getBoolean(KEY_TIME_24H, true);
        int trashDays = preferences.getInt(KEY_TRASH_AUTO_DELETE_DAYS, 30);

        if (switchDarkMode != null) switchDarkMode.setChecked(darkMode);
        if (switchSound != null) switchSound.setChecked(sound);
        if (switchVibrate != null) switchVibrate.setChecked(vibrate);
        if (switchTime24h != null) switchTime24h.setChecked(time24h);

        // Sort label
        if (textSortOrder != null) {
            switch (sortOrder) {
                case "date_asc": textSortOrder.setText("По дате (старые сверху)"); break;
                case "title": textSortOrder.setText("По заголовку"); break;
                default: textSortOrder.setText("По дате (новые сверху)");
            }
        }

        // Default reminder label
        if (textDefaultReminder != null) {
            textDefaultReminder.setText(defaultReminder == 0 ? "Не установлено" : defaultReminder + " мин");
        }

        // Trash auto-delete label
        if (textTrashAutoDelete != null) {
            if (trashDays == 0) {
                textTrashAutoDelete.setText("Никогда");
            } else {
                textTrashAutoDelete.setText(trashDays + " дней");
            }
        }
    }

    // AUTO-DELETE DIALOG - Alternative approach that will definitely work!
    private void showTrashAutoDeleteDialog() {
        final String[] options = {"Никогда", "7 дней", "30 дней", "60 дней", "90 дней"};
        final int[] values = {0, 7, 30, 60, 90};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Автоматическое удаление из корзины");

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Save the selected option
                preferences.edit().putInt(KEY_TRASH_AUTO_DELETE_DAYS, values[which]).apply();

                // Update the display text
                if (textTrashAutoDelete != null) {
                    textTrashAutoDelete.setText(options[which]);
                }

                // Perform cleanup now if a time limit is set
                if (values[which] > 0) {
                    int deletedCount = databaseHelper.cleanupOldTrashNotes(values[which]);
                    updateCounts();
                    String message = "Настройка сохранена.";
                    if (deletedCount > 0) {
                        message += " Удалено старых заметок: " + deletedCount;
                    }
                    Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SettingsActivity.this,
                            "Автоудаление отключено", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Empty trash dialog from settings
    private void showEmptyTrashDialog() {
        int trashCount = databaseHelper.getTrashCount();
        if (trashCount == 0) {
            Toast.makeText(this, "Корзина уже пуста", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Очистить корзину?")
                .setMessage("В корзине " + trashCount + " заметок. Все они будут удалены навсегда. Это действие нельзя отменить.")
                .setPositiveButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseHelper.emptyTrash();
                        updateCounts();
                        Toast.makeText(SettingsActivity.this, "Корзина очищена", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
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
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String sortOrder = which == 1 ? "date_asc" : which == 2 ? "title" : "date_desc";
                        preferences.edit().putString(KEY_SORT_ORDER, sortOrder).apply();
                        if (textSortOrder != null) {
                            textSortOrder.setText(options[which]);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showDefaultReminderDialog() {
        final String[] options = {"Не установлено", "5 мин", "15 мин", "30 мин", "1 час", "1 день"};
        final int[] values = {0, 5, 15, 30, 60, 1440};
        int currentValue = preferences.getInt(KEY_DEFAULT_REMINDER, 0);
        int checkedItem = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentValue) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Напоминание по умолчанию")
                .setSingleChoiceItems(options, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().putInt(KEY_DEFAULT_REMINDER, values[which]).apply();
                        if (textDefaultReminder != null) {
                            textDefaultReminder.setText(options[which]);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateCounts() {
        int notesCount = databaseHelper.getAllNotes().size();
        int trashCount = databaseHelper.getTrashCount();

        if (textNotesCount != null) {
            textNotesCount.setText("Всего заметок: " + notesCount);
        }
        if (textTrashCount != null) {
            textTrashCount.setText("В корзине: " + trashCount);
        }
    }

    private void exportNotes() {
        // Implement export functionality
        Toast.makeText(this, "Экспорт заметок (в разработке)", Toast.LENGTH_SHORT).show();
    }

    private void importNotes() {
        // Implement import functionality
        Toast.makeText(this, "Импорт заметок (в разработке)", Toast.LENGTH_SHORT).show();
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить все заметки?")
                .setMessage("Все заметки будут перемещены в корзину. Это действие можно будет отменить.")
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseHelper.moveAllToTrash();
                        updateCounts();
                        Toast.makeText(SettingsActivity.this, "Все заметки перемещены в корзину", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("О приложении")
                .setMessage("NoteApp v1.0\n\nПростое и удобное приложение для заметок с возможностью категоризации, напоминаний и корзины.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCounts(); // Refresh counts when returning to settings
    }
}