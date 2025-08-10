package com.kelo.noteapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Bump DB version to add repeat_days
    private static final int DATABASE_VERSION = 3;
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
    private static final String COLUMN_REPEAT_DAYS = "repeat_days"; // NEW

    // Create
    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT NOT NULL,"
            + COLUMN_CONTENT + " TEXT,"
            + COLUMN_CREATED_AT + " INTEGER,"
            + COLUMN_REMINDER_TIME + " INTEGER DEFAULT 0,"
            + COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0,"
            + COLUMN_IS_PINNED + " INTEGER DEFAULT 0,"
            + COLUMN_REPEAT_DAYS + " INTEGER DEFAULT 0"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v2 added is_pinned
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_IS_PINNED + " INTEGER DEFAULT 0");
        }
        // v3 adds repeat_days
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_REPEAT_DAYS + " INTEGER DEFAULT 0");
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
        values.put(COLUMN_REPEAT_DAYS, note.getRepeatDays());
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    // Read one
    Note getNote(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NOTES,
                new String[]{COLUMN_ID, COLUMN_TITLE, COLUMN_CONTENT, COLUMN_CREATED_AT,
                        COLUMN_REMINDER_TIME, COLUMN_IS_COMPLETED, COLUMN_IS_PINNED, COLUMN_REPEAT_DAYS},
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null
        );

        Note note = null;
        if (c != null && c.moveToFirst()) {
            note = new Note();
            note.setId(c.getInt(c.getColumnIndex(COLUMN_ID)));
            note.setTitle(c.getString(c.getColumnIndex(COLUMN_TITLE)));
            note.setContent(c.getString(c.getColumnIndex(COLUMN_CONTENT)));
            note.setCreatedAt(c.getLong(c.getColumnIndex(COLUMN_CREATED_AT)));
            note.setReminderTime(c.getLong(c.getColumnIndex(COLUMN_REMINDER_TIME)));
            note.setCompleted(c.getInt(c.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
            note.setPinned(c.getInt(c.getColumnIndex(COLUMN_IS_PINNED)) == 1);
            note.setRepeatDays(c.getInt(c.getColumnIndex(COLUMN_REPEAT_DAYS)));
        }
        if (c != null) c.close();
        db.close();
        return note;
    }

    // Read all — pinned first, then not completed, then newest
    List<Note> getAllNotes() {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NOTES +
                " ORDER BY " + COLUMN_IS_PINNED + " DESC, " +
                COLUMN_IS_COMPLETED + " ASC, " +
                COLUMN_CREATED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do {
                Note n = new Note();
                n.setId(c.getInt(c.getColumnIndex(COLUMN_ID)));
                n.setTitle(c.getString(c.getColumnIndex(COLUMN_TITLE)));
                n.setContent(c.getString(c.getColumnIndex(COLUMN_CONTENT)));
                n.setCreatedAt(c.getLong(c.getColumnIndex(COLUMN_CREATED_AT)));
                n.setReminderTime(c.getLong(c.getColumnIndex(COLUMN_REMINDER_TIME)));
                n.setCompleted(c.getInt(c.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
                n.setPinned(c.getInt(c.getColumnIndex(COLUMN_IS_PINNED)) == 1);
                n.setRepeatDays(c.getInt(c.getColumnIndex(COLUMN_REPEAT_DAYS)));
                list.add(n);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // Active notes (example)
    List<Note> getActiveNotes() {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_COMPLETED + " = 0 " +
                " ORDER BY " + COLUMN_IS_PINNED + " DESC, " + COLUMN_CREATED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do {
                Note n = new Note();
                n.setId(c.getInt(c.getColumnIndex(COLUMN_ID)));
                n.setTitle(c.getString(c.getColumnIndex(COLUMN_TITLE)));
                n.setContent(c.getString(c.getColumnIndex(COLUMN_CONTENT)));
                n.setCreatedAt(c.getLong(c.getColumnIndex(COLUMN_CREATED_AT)));
                n.setReminderTime(c.getLong(c.getColumnIndex(COLUMN_REMINDER_TIME)));
                n.setCompleted(c.getInt(c.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
                n.setPinned(c.getInt(c.getColumnIndex(COLUMN_IS_PINNED)) == 1);
                n.setRepeatDays(c.getInt(c.getColumnIndex(COLUMN_REPEAT_DAYS)));
                list.add(n);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
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
        values.put(COLUMN_REPEAT_DAYS, note.getRepeatDays());
        int rows = db.update(TABLE_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(note.getId())});
        db.close();
        return rows;
    }

    // Delete
    void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Extra utils already used in Settings
    void deleteAllNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, null, null);
        db.close();
    }

    int getNotesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOTES, null);
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
        }
        db.close();
        return count;
    }
}
