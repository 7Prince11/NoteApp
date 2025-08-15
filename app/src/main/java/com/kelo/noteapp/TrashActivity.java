package com.kelo.noteapp;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class TrashActivity extends AppCompatActivity implements TrashAdapter.OnTrashListener {

    private RecyclerView recyclerView;
    private TrashAdapter trashAdapter;
    private List<Note> trashNotes;
    private TextView emptyView;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Корзина");
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        databaseHelper = new DatabaseHelper(this);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        loadTrashNotes();

        // Auto-cleanup old notes based on user preference
        performAutoCleanup();
    }

    private void loadTrashNotes() {
        trashNotes = databaseHelper.getTrashNotes();

        if (trashNotes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            trashAdapter = new TrashAdapter(this, trashNotes, this);
            recyclerView.setAdapter(trashAdapter);
        }
    }

    private void performAutoCleanup() {
        SharedPreferences prefs = getSharedPreferences("NotesAppPrefs", MODE_PRIVATE);
        int trashDays = prefs.getInt("trash_auto_delete_days", 30); // Default 30 days

        if (trashDays > 0) {
            databaseHelper.cleanupOldTrashNotes(trashDays);
        }
    }

    @Override
    public void onRestoreClick(int position) {
        Note note = trashNotes.get(position);

        // Restore note from trash
        databaseHelper.restoreFromTrash(note.getId());

        // Remove from list
        trashNotes.remove(position);
        trashAdapter.notifyItemRemoved(position);

        // Update empty state
        if (trashNotes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }

        // Show success message with undo option
        Snackbar.make(recyclerView, "Заметка восстановлена", Snackbar.LENGTH_LONG)
                .setAction("ОТМЕНИТЬ", v -> {
                    // Move back to trash
                    databaseHelper.moveToTrash(note.getId());
                    trashNotes.add(position, note);
                    trashAdapter.notifyItemInserted(position);

                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                })
                .show();
    }

    @Override
    public void onPermanentDeleteClick(int position) {
        Note note = trashNotes.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Удалить навсегда?")
                .setMessage("Эта заметка будет удалена безвозвратно. Продолжить?")
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Permanently delete
                        databaseHelper.permanentlyDeleteNote(note.getId());

                        // Remove from list
                        trashNotes.remove(position);
                        trashAdapter.notifyItemRemoved(position);

                        // Update empty state
                        if (trashNotes.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        }

                        Toast.makeText(TrashActivity.this, "Заметка удалена навсегда", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_empty_trash) {
            showEmptyTrashDialog();
            return true;
        } else if (id == R.id.action_restore_all) {
            showRestoreAllDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showEmptyTrashDialog() {
        if (trashNotes.isEmpty()) {
            Toast.makeText(this, "Корзина уже пуста", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Очистить корзину?")
                .setMessage("Все заметки в корзине будут удалены навсегда. Это действие нельзя отменить.")
                .setPositiveButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseHelper.emptyTrash();
                        trashNotes.clear();
                        trashAdapter.notifyDataSetChanged();

                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);

                        Toast.makeText(TrashActivity.this, "Корзина очищена", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showRestoreAllDialog() {
        if (trashNotes.isEmpty()) {
            Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Восстановить все заметки?")
                .setMessage("Все заметки в корзине будут восстановлены.")
                .setPositiveButton("Восстановить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Restore all notes
                        for (Note note : trashNotes) {
                            databaseHelper.restoreFromTrash(note.getId());
                        }

                        int count = trashNotes.size();
                        trashNotes.clear();
                        trashAdapter.notifyDataSetChanged();

                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);

                        Toast.makeText(TrashActivity.this,
                                "Восстановлено заметок: " + count, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrashNotes(); // Refresh in case notes were cleaned up
    }
}