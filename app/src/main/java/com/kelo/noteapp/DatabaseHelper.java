package com.kelo.noteapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {

    // ⬆ Bumped to v4: adds COLUMN_CATEGORY. All older features untouched.
    private static final int DATABASE_VERSION = 4;
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
    private static final String COLUMN_REPEAT_DAYS = "repeat_days";
    // NEW:
    private static final String COLUMN_CATEGORY = "category";

    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT NOT NULL,"
            + COLUMN_CONTENT + " TEXT,"
            + COLUMN_CREATED_AT + " INTEGER,"
            + COLUMN_REMINDER_TIME + " INTEGER DEFAULT 0,"
            + COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0,"
            + COLUMN_IS_PINNED + " INTEGER DEFAULT 0,"
            + COLUMN_REPEAT_DAYS + " INTEGER DEFAULT 0,"
            + COLUMN_CATEGORY + " TEXT DEFAULT 'personal'"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v2: is_pinned
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_IS_PINNED + " INTEGER DEFAULT 0");
        }
        // v3: repeat_days
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_REPEAT_DAYS + " INTEGER DEFAULT 0");
        }
        // v4: category
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_CATEGORY + " TEXT DEFAULT 'personal'");
        }
    }

    // --- CREATE ---
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
        values.put(COLUMN_CATEGORY, note.getCategory() == null ? "personal" : note.getCategory());
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    // --- READ ONE ---
    Note getNote(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NOTES,
                null,
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null
        );

        Note note = null;
        if (c != null && c.moveToFirst()) {
            note = readNoteFromCursor(c);
        }
        if (c != null) c.close();
        db.close();
        return note;
    }

    // --- READ ALL (pinned first, active first, newest) ---
    List<Note> getAllNotes() {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NOTES +
                " ORDER BY " + COLUMN_IS_PINNED + " DESC, " +
                COLUMN_IS_COMPLETED + " ASC, " +
                COLUMN_CREATED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do { list.add(readNoteFromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // --- FILTER BY CATEGORY (keeps your sort) ---
    List<Note> getNotesByCategory(String categoryKey) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NOTES, null,
                COLUMN_CATEGORY + "=?",
                new String[]{categoryKey},
                null, null,
                COLUMN_IS_PINNED + " DESC, " + COLUMN_IS_COMPLETED + " ASC, " + COLUMN_CREATED_AT + " DESC");
        if (c.moveToFirst()) {
            do { list.add(readNoteFromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // --- CALENDAR HELPERS (unchanged) ---
    List<Note> getNotesForDate(int year, int month, int day) {
        List<Note> list = new ArrayList<>();

        Calendar dateCal = Calendar.getInstance();
        dateCal.set(year, month, day, 0, 0, 0);
        dateCal.set(Calendar.MILLISECOND, 0);
        long startTime = dateCal.getTimeInMillis();

        Calendar endCal = Calendar.getInstance();
        endCal.set(year, month, day, 23, 59, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        long endTime = endCal.getTimeInMillis();

        long currentTime = System.currentTimeMillis();
        if (startTime < currentTime) startTime = currentTime;

        String sql = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_REMINDER_TIME + " > 0 OR " + COLUMN_REPEAT_DAYS + " != 0" +
                " ORDER BY " + COLUMN_IS_PINNED + " DESC, " +
                COLUMN_IS_COMPLETED + " ASC, " +
                COLUMN_CREATED_AT + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);

        if (c.moveToFirst()) {
            do {
                Note n = readNoteFromCursor(c);

                if (n.getReminderTime() >= startTime && n.getReminderTime() <= endTime) {
                    list.add(n);
                } else if (n.getRepeatDays() != 0 && startTime >= currentTime) {
                    int dayOfWeek = dateCal.get(Calendar.DAY_OF_WEEK);
                    int bitIndex = convertDayOfWeekToBitIndex(dayOfWeek);
                    if (((n.getRepeatDays() >> bitIndex) & 1) == 1) {
                        list.add(n);
                    }
                }
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    Map<String, Integer> getNotesCountForMonth(int year, int month) {
        Map<String, Integer> countMap = new HashMap<>();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayStart = today.getTimeInMillis();

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_REMINDER_TIME + " > 0 AND " +
                COLUMN_IS_COMPLETED + " = 0", null);

        if (c.moveToFirst()) {
            do {
                long reminderTime = c.getLong(c.getColumnIndex(COLUMN_REMINDER_TIME));
                if (reminderTime >= todayStart) {
                    Calendar reminderCal = Calendar.getInstance();
                    reminderCal.setTimeInMillis(reminderTime);

                    if (reminderCal.get(Calendar.YEAR) == year &&
                            reminderCal.get(Calendar.MONTH) == month) {
                        String dateKey = String.format("%04d-%02d-%02d",
                                year, month + 1, reminderCal.get(Calendar.DAY_OF_MONTH));
                        countMap.put(dateKey, countMap.getOrDefault(dateKey, 0) + 1);
                    }
                }
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        return countMap;
    }

    Set<String> getRecurringDatesForMonth(int year, int month) {
        Set<String> recurringDates = new HashSet<>();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar weekAhead = (Calendar) today.clone();
        weekAhead.add(Calendar.DAY_OF_MONTH, 7);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_REPEAT_DAYS + " != 0 AND " +
                COLUMN_IS_COMPLETED + " = 0", null);

        if (c.moveToFirst()) {
            do {
                int repeatDays = c.getInt(c.getColumnIndex(COLUMN_REPEAT_DAYS));

                Calendar checkDate = (Calendar) today.clone();
                for (int i = 0; i < 7; i++) {
                    if (checkDate.get(Calendar.YEAR) == year &&
                            checkDate.get(Calendar.MONTH) == month) {

                        int dayOfWeek = checkDate.get(Calendar.DAY_OF_WEEK);
                        int bitIndex = convertDayOfWeekToBitIndex(dayOfWeek);

                        if (((repeatDays >> bitIndex) & 1) == 1) {
                            String dateKey = String.format("%04d-%02d-%02d",
                                    checkDate.get(Calendar.YEAR),
                                    checkDate.get(Calendar.MONTH) + 1,
                                    checkDate.get(Calendar.DAY_OF_MONTH));
                            recurringDates.add(dateKey);
                        }
                    }
                    checkDate.add(Calendar.DAY_OF_MONTH, 1);
                }
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        return recurringDates;
    }

    private int convertDayOfWeekToBitIndex(int calendarDayOfWeek) {
        switch (calendarDayOfWeek) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
            default: return 0;
        }
    }

    // Active notes (unchanged)
    List<Note> getActiveNotes() {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_COMPLETED + " = 0 " +
                " ORDER BY " + COLUMN_IS_PINNED + " DESC, " + COLUMN_CREATED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do { list.add(readNoteFromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // --- UPDATE ---
    int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_REMINDER_TIME, note.getReminderTime());
        values.put(COLUMN_IS_COMPLETED, note.isCompleted() ? 1 : 0);
        values.put(COLUMN_IS_PINNED, note.isPinned() ? 1 : 0);
        values.put(COLUMN_REPEAT_DAYS, note.getRepeatDays());
        values.put(COLUMN_CATEGORY, note.getCategory() == null ? "personal" : note.getCategory());
        int rows = db.update(TABLE_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(note.getId())});
        db.close();
        return rows;
    }

    // --- DELETE ---
    void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // --- UTILITIES ---
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

    // ---- internal helper ----
    private Note readNoteFromCursor(Cursor c) {
        Note n = new Note();
        n.setId(c.getInt(c.getColumnIndex(COLUMN_ID)));
        n.setTitle(c.getString(c.getColumnIndex(COLUMN_TITLE)));
        n.setContent(c.getString(c.getColumnIndex(COLUMN_CONTENT)));
        n.setCreatedAt(c.getLong(c.getColumnIndex(COLUMN_CREATED_AT)));
        n.setReminderTime(c.getLong(c.getColumnIndex(COLUMN_REMINDER_TIME)));
        n.setCompleted(c.getInt(c.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
        n.setPinned(c.getInt(c.getColumnIndex(COLUMN_IS_PINNED)) == 1);
        n.setRepeatDays(c.getInt(c.getColumnIndex(COLUMN_REPEAT_DAYS)));
        // NEW:
        int idxCat = c.getColumnIndex(COLUMN_CATEGORY);
        n.setCategory(idxCat >= 0 ? c.getString(idxCat) : "personal");
        return n;
    }
}
