package com.kelo.noteapp;



import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "NotesDatabase.db";

    // Таблица заметок
    private static final String TABLE_NOTES = "notes";

    // Колонки таблицы
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_REMINDER_TIME = "reminder_time";
    private static final String COLUMN_IS_COMPLETED = "is_completed";

    // SQL запрос для создания таблицы
    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT NOT NULL,"
            + COLUMN_CONTENT + " TEXT,"
            + COLUMN_CREATED_AT + " INTEGER,"
            + COLUMN_REMINDER_TIME + " INTEGER DEFAULT 0,"
            + COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Удаление старой таблицы если существует
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        // Создание новой таблицы
        onCreate(db);
    }

    // Добавление новой заметки
    public long addNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_CREATED_AT, note.getCreatedAt());
        values.put(COLUMN_REMINDER_TIME, note.getReminderTime());
        values.put(COLUMN_IS_COMPLETED, note.isCompleted() ? 1 : 0);

        long id = db.insert(TABLE_NOTES, null, values);
        db.close();

        return id;
    }

    // Получение одной заметки по ID
    public Note getNote(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NOTES,
                new String[]{COLUMN_ID, COLUMN_TITLE, COLUMN_CONTENT,
                        COLUMN_CREATED_AT, COLUMN_REMINDER_TIME, COLUMN_IS_COMPLETED},
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Note note = new Note();
            note.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
            note.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
            note.setContent(cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)));
            note.setCreatedAt(cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT)));
            note.setReminderTime(cursor.getLong(cursor.getColumnIndex(COLUMN_REMINDER_TIME)));
            note.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);

            cursor.close();
            return note;
        }

        return null;
    }

    // Получение всех заметок
    public List<Note> getAllNotes() {
        List<Note> notesList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NOTES +
                " ORDER BY " + COLUMN_IS_COMPLETED + " ASC, " +
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

                notesList.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return notesList;
    }

    // Получение активных заметок (невыполненных)
    public List<Note> getActiveNotes() {
        List<Note> notesList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_COMPLETED + " = 0" +
                " ORDER BY " + COLUMN_CREATED_AT + " DESC";

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
                note.setCompleted(false);

                notesList.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return notesList;
    }

    // Обновление заметки
    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_REMINDER_TIME, note.getReminderTime());
        values.put(COLUMN_IS_COMPLETED, note.isCompleted() ? 1 : 0);

        int rowsAffected = db.update(TABLE_NOTES, values,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(note.getId())});

        db.close();
        return rowsAffected;
    }

    // Удаление заметки
    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    // Удаление всех заметок
    public void deleteAllNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, null, null);
        db.close();
    }

    // Получение количества заметок
    public int getNotesCount() {
        String countQuery = "SELECT * FROM " + TABLE_NOTES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }
}