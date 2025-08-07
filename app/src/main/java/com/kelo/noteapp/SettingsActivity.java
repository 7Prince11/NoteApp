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

    private SharedPreferences preferences;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchSound;
    private SwitchMaterial switchVibrate;
    private TextView textSortOrder;
    private TextView textDefaultReminder;
    private TextView textNotesCount;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Настройка toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Настройки");

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(this);

        // Инициализация views
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchSound = findViewById(R.id.switchSound);
        switchVibrate = findViewById(R.id.switchVibrate);
        textSortOrder = findViewById(R.id.textSortOrder);
        textDefaultReminder = findViewById(R.id.textDefaultReminder);
        textNotesCount = findViewById(R.id.textNotesCount);

        // Загрузка настроек
        loadSettings();

        // Обработчики переключателей
        switchDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();

                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }

                recreate(); // Пересоздать активность для применения темы
            }
        });

        switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_NOTIFICATION_SOUND, isChecked).apply();
            }
        });

        switchVibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_NOTIFICATION_VIBRATE, isChecked).apply();
            }
        });

        // Обработчик выбора сортировки
        findViewById(R.id.layoutSortOrder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortOrderDialog();
            }
        });

        // Обработчик выбора времени напоминания по умолчанию
        findViewById(R.id.layoutDefaultReminder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDefaultReminderDialog();
            }
        });

        // Экспорт заметок
        findViewById(R.id.layoutExport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportNotes();
            }
        });

        // Импорт заметок
        findViewById(R.id.layoutImport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importNotes();
            }
        });

        // Очистка всех заметок
        findViewById(R.id.layoutClearAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearAllDialog();
            }
        });

        // О приложении
        findViewById(R.id.layoutAbout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        });

        // Обновление счетчика заметок
        updateNotesCount();
    }

    private void loadSettings() {
        boolean darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        boolean sound = preferences.getBoolean(KEY_NOTIFICATION_SOUND, true);
        boolean vibrate = preferences.getBoolean(KEY_NOTIFICATION_VIBRATE, true);
        String sortOrder = preferences.getString(KEY_SORT_ORDER, "date_desc");
        int defaultReminder = preferences.getInt(KEY_DEFAULT_REMINDER, 0);

        switchDarkMode.setChecked(darkMode);
        switchSound.setChecked(sound);
        switchVibrate.setChecked(vibrate);

        // Обновление текста сортировки
        switch (sortOrder) {
            case "date_desc":
                textSortOrder.setText("По дате (новые сверху)");
                break;
            case "date_asc":
                textSortOrder.setText("По дате (старые сверху)");
                break;
            case "title":
                textSortOrder.setText("По заголовку");
                break;
        }

        // Обновление текста напоминания
        if (defaultReminder == 0) {
            textDefaultReminder.setText("Не установлено");
        } else {
            textDefaultReminder.setText(defaultReminder + " мин");
        }
    }

    private void showSortOrderDialog() {
        String[] options = {"По дате (новые сверху)", "По дате (старые сверху)", "По заголовку"};
        String currentSort = preferences.getString(KEY_SORT_ORDER, "date_desc");
        int checkedItem = 0;

        if (currentSort.equals("date_asc")) checkedItem = 1;
        else if (currentSort.equals("title")) checkedItem = 2;

        new AlertDialog.Builder(this)
                .setTitle("Сортировка заметок")
                .setSingleChoiceItems(options, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String sortOrder = "date_desc";
                        switch (which) {
                            case 1:
                                sortOrder = "date_asc";
                                break;
                            case 2:
                                sortOrder = "title";
                                break;
                        }

                        preferences.edit().putString(KEY_SORT_ORDER, sortOrder).apply();
                        textSortOrder.setText(options[which]);
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
                        textDefaultReminder.setText(options[which].replace(" минут", " мин"));
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void exportNotes() {
        // Здесь будет логика экспорта заметок в файл
        Toast.makeText(this, "Функция экспорта в разработке", Toast.LENGTH_SHORT).show();
    }

    private void importNotes() {
        // Здесь будет логика импорта заметок из файла
        Toast.makeText(this, "Функция импорта в разработке", Toast.LENGTH_SHORT).show();
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить все заметки?")
                .setMessage("Это действие удалит все заметки безвозвратно. Продолжить?")
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseHelper.deleteAllNotes();
                        updateNotesCount();
                        Toast.makeText(SettingsActivity.this,
                                "Все заметки удалены", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("О приложении")
                .setMessage("Мои Заметки v1.0\n\n" +
                        "Простое и удобное приложение для создания заметок с напоминаниями.\n\n" +
                        "© 2024 Example Company")
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateNotesCount() {
        int count = databaseHelper.getNotesCount();
        textNotesCount.setText("Всего заметок: " + count);
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