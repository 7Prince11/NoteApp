package com.kelo.noteapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2; // bumped
    private static final String DATABASE_NAME = "NotesDatabase.db";

    // Table & columns
    private static final String TABLE_NOTES = "notes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_REMINDER_TIME = "reminder_time";
    private static final String COLUMN_IS_COMPLETED = "is_completed";
    private static final String COLUMN_IS_PINNED = "is_pinned";

    // Create table
    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT NOT NULL,"
            + COLUMN_CONTENT + " TEXT,"
            + COLUMN_CREATED_AT + " INTEGER,"
            + COLUMN_REMINDER_TIME + " INTEGER DEFAULT 0,"
            + COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0,"
            + COLUMN_IS_PINNED + " INTEGER DEFAULT 0"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migration to v2: add is_pinned column (preserve data)
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_IS_PINNED + " INTEGER DEFAULT 0");
        }
    }

    // Create
    long addNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_CREATED_AT, note.getCreatedAt());
        values.put(COLUMN_REMINDER_TIME, note.getReminderTime());
        values.put(COLUMN_IS_COMPLETED, note.isCompleted() ? 1 : 0);
        values.put(COLUMN_IS_PINNED, note.isPinned() ? 1 : 0);

        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    // Read one
    Note getNote(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_NOTES,
                new String[]{COLUMN_ID, COLUMN_TITLE, COLUMN_CONTENT,
                        COLUMN_CREATED_AT, COLUMN_REMINDER_TIME, COLUMN_IS_COMPLETED, COLUMN_IS_PINNED},
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            Note note = new Note();
            note.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
            note.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
            note.setContent(cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)));
            note.setCreatedAt(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT)));
            note.setReminderTime(cursor.getLong(cursor.getColumnIndex(COLUMN_REMINDER_TIME)));
            note.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
            note.setPinned(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_PINNED)) == 1);
            cursor.close();
            return note;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    // Read all — pinned first, then not completed, then newest
    List<Note> getAllNotes() {
        List<Note> notesList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NOTES +
                " ORDER BY " + COLUMN_IS_PINNED + " DESC, " +
                COLUMN_IS_COMPLETED + " ASC, " +
                COLUMN_CREATED_AT + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)));
                note.setCreatedAt(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT)));
                note.setReminderTime(cursor.getLong(cursor.getColumnIndex(COLUMN_REMINDER_TIME)));
                note.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
                note.setPinned(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_PINNED)) == 1);
                notesList.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return notesList;
    }

    // Read active (not completed), still ordered with pinned first
    List<Note> getActiveNotes() {
        List<Note> notesList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_COMPLETED + " = 0 " +
                " ORDER BY " + COLUMN_IS_PINNED + " DESC, " +
                COLUMN_CREATED_AT + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)));
                note.setCreatedAt(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT)));
                note.setReminderTime(cursor.getLong(cursor.getColumnIndex(COLUMN_REMINDER_TIME)));
                note.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
                note.setPinned(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_PINNED)) == 1);
                notesList.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return notesList;
    }

    // Update
    int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_REMINDER_TIME, note.getReminderTime());
        values.put(COLUMN_IS_COMPLETED, note.isCompleted() ? 1 : 0);
        values.put(COLUMN_IS_PINNED, note.isPinned() ? 1 : 0);

        int rowsAffected = db.update(
                TABLE_NOTES,
                values,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(note.getId())}
        );

        db.close();
        return rowsAffected;
    }
    // Удалить все заметки
    void deleteAllNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, null, null);
        db.close();
    }

    // Количество заметок
    int getNotesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOTES, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return count;
    }
    // Delete
    void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
