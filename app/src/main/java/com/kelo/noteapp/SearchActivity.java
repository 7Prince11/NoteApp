package com.kelo.noteapp;


import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;


public class SearchActivity extends AppCompatActivity implements NoteAdapter.OnNoteListener {

    private EditText searchEditText;
    private ImageButton clearButton;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private NoteAdapter noteAdapter;
    private List<Note> allNotes;
    private List<Note> filteredNotes;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Настройка toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Поиск заметок");

        // Инициализация views
        searchEditText = findViewById(R.id.searchEditText);
        clearButton = findViewById(R.id.clearButton);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        databaseHelper = new DatabaseHelper(this);

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Загрузка всех заметок
        allNotes = databaseHelper.getAllNotes();
        filteredNotes = new ArrayList<>();

        // Настройка адаптера
        noteAdapter = new NoteAdapter(this, filteredNotes, this);
        recyclerView.setAdapter(noteAdapter);

        // Показать пустое состояние изначально
        showEmptyState(true);

        // Обработчик ввода текста
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
                clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Обработчик кнопки очистки
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEditText.setText("");
            }
        });

        // Автоматически открыть клавиатуру
        searchEditText.requestFocus();
    }

    private void filterNotes(String query) {
        filteredNotes.clear();

        if (query.isEmpty()) {
            showEmptyState(true);
            emptyView.setText("Введите текст для поиска");
        } else {
            String lowerQuery = query.toLowerCase();

            for (Note note : allNotes) {
                if (note.getTitle().toLowerCase().contains(lowerQuery) ||
                        note.getContent().toLowerCase().contains(lowerQuery)) {
                    filteredNotes.add(note);
                }
            }

            if (filteredNotes.isEmpty()) {
                showEmptyState(true);
                emptyView.setText("Ничего не найдено по запросу: \"" + query + "\"");
            } else {
                showEmptyState(false);
            }
        }

        noteAdapter.notifyDataSetChanged();
    }

    private void showEmptyState(boolean show) {
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNoteClick(int position) {
        Note note = filteredNotes.get(position);
        Intent intent = new Intent(SearchActivity.this, AddEditNoteActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());
        intent.putExtra("note_reminder", note.getReminderTime());
        startActivity(intent);
        finish();
    }

    @Override
    public void onDeleteClick(int position) {
        Note note = filteredNotes.get(position);
        databaseHelper.deleteNote(note.getId());
        filteredNotes.remove(position);
        allNotes.remove(note);
        noteAdapter.notifyItemRemoved(position);

        if (filteredNotes.isEmpty()) {
            showEmptyState(true);
            emptyView.setText("Ничего не найдено");
        }
    }

    @Override
    public void onCompleteClick(int position) {
        Note note = filteredNotes.get(position);
        note.setCompleted(!note.isCompleted());
        databaseHelper.updateNote(note);
        noteAdapter.notifyItemChanged(position);
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