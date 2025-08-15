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

    // Database version bumped to v5: adds COLUMN_IS_DELETED and COLUMN_DELETED_AT for trash functionality
    private static final int DATABASE_VERSION = 5;
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
    private static final String COLUMN_CATEGORY = "category";
    // Trash functionality columns
    private static final String COLUMN_IS_DELETED = "is_deleted";
    private static final String COLUMN_DELETED_AT = "deleted_at";

    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT NOT NULL,"
            + COLUMN_CONTENT + " TEXT,"
            + COLUMN_CREATED_AT + " INTEGER,"
            + COLUMN_REMINDER_TIME + " INTEGER DEFAULT 0,"
            + COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0,"
            + COLUMN_IS_PINNED + " INTEGER DEFAULT 0,"
            + COLUMN_REPEAT_DAYS + " INTEGER DEFAULT 0,"
            + COLUMN_CATEGORY + " TEXT DEFAULT 'personal',"
            + COLUMN_IS_DELETED + " INTEGER DEFAULT 0,"
            + COLUMN_DELETED_AT + " INTEGER DEFAULT 0"
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
        // v5: trash functionality
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_IS_DELETED + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_DELETED_AT + " INTEGER DEFAULT 0");
        }
    }

    // ===== CREATE OPERATIONS =====

    public long addNote(Note note) {
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
        values.put(COLUMN_IS_DELETED, 0); // New notes are not deleted
        values.put(COLUMN_DELETED_AT, 0);
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    // ===== READ OPERATIONS =====

    public Note getNote(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NOTES,
                null,
                COLUMN_ID + "=? AND " + COLUMN_IS_DELETED + "=0", // Only non-deleted notes
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

    public List<Note> getAllNotes() {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_DELETED + "=0" +
                " ORDER BY " + COLUMN_IS_PINNED + " DESC, " +
                COLUMN_IS_COMPLETED + " ASC, " +
                COLUMN_CREATED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do {
                list.add(readNoteFromCursor(c));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public List<Note> getNotesByCategory(String categoryKey) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NOTES, null,
                COLUMN_CATEGORY + "=? AND " + COLUMN_IS_DELETED + "=0",
                new String[]{categoryKey},
                null, null,
                COLUMN_IS_PINNED + " DESC, " + COLUMN_IS_COMPLETED + " ASC, " + COLUMN_CREATED_AT + " DESC");
        if (c.moveToFirst()) {
            do {
                list.add(readNoteFromCursor(c));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public List<Note> getActiveNotes() {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_COMPLETED + " = 0 AND " + COLUMN_IS_DELETED + " = 0" +
                " ORDER BY " + COLUMN_IS_PINNED + " DESC, " + COLUMN_CREATED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do {
                list.add(readNoteFromCursor(c));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // ===== UPDATE OPERATIONS =====

    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_REMINDER_TIME, note.getReminderTime());
        values.put(COLUMN_IS_COMPLETED, note.isCompleted() ? 1 : 0);
        values.put(COLUMN_IS_PINNED, note.isPinned() ? 1 : 0);
        values.put(COLUMN_REPEAT_DAYS, note.getRepeatDays());
        values.put(COLUMN_CATEGORY, note.getCategory() == null ? "personal" : note.getCategory());
        // Note: we don't update is_deleted or deleted_at during normal updates
        int rows = db.update(TABLE_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(note.getId())});
        db.close();
        return rows;
    }

    // ===== DELETE OPERATIONS =====

    public void deleteNote(int id) {
        moveToTrash(id); // Use soft delete by default
    }

    public void deleteAllNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Move all non-deleted notes to trash
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DELETED, 1);
        values.put(COLUMN_DELETED_AT, System.currentTimeMillis());
        db.update(TABLE_NOTES, values, COLUMN_IS_DELETED + " = 0", null);
        db.close();
    }

    // ===== TRASH FUNCTIONALITY =====

    public List<Note> getTrashNotes() {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_DELETED + "=1" +
                " ORDER BY " + COLUMN_DELETED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do {
                list.add(readNoteFromCursor(c));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public void moveToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DELETED, 1);
        values.put(COLUMN_DELETED_AT, System.currentTimeMillis());
        db.update(TABLE_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void restoreFromTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DELETED, 0);
        values.put(COLUMN_DELETED_AT, 0);
        db.update(TABLE_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void permanentlyDeleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void emptyTrash() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_IS_DELETED + " = 1", null);
        db.close();
    }

    public int cleanupOldTrashNotes(int daysToKeep) {
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        SQLiteDatabase db = this.getWritableDatabase();

        int deletedCount = db.delete(TABLE_NOTES,
                COLUMN_IS_DELETED + " = 1 AND " + COLUMN_DELETED_AT + " < ?",
                new String[]{String.valueOf(cutoffTime)});

        db.close();
        return deletedCount; // Return how many notes were actually deleted
    }

    public void moveAllToTrash() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DELETED, 1);
        values.put(COLUMN_DELETED_AT, System.currentTimeMillis());
        db.update(TABLE_NOTES, values, COLUMN_IS_DELETED + " = 0", null);
        db.close();
    }

    public int getTrashCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + TABLE_NOTES + " WHERE " + COLUMN_IS_DELETED + "=1";
        Cursor cursor = db.rawQuery(sql, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    // ===== UTILITY METHODS =====

    public int getNotesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOTES + " WHERE " + COLUMN_IS_DELETED + " = 0", null);
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
        }
        db.close();
        return count;
    }

    // ===== CALENDAR & REMINDER FUNCTIONALITY =====

    public List<Note> getNotesForDate(String dateKey) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Get the start and end of the date
        String[] parts = dateKey.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1; // Calendar months are 0-based
        int day = Integer.parseInt(parts[2]);

        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(year, month, day, 0, 0, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(year, month, day, 23, 59, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        long startTime = startOfDay.getTimeInMillis();
        long endTime = endOfDay.getTimeInMillis();

        String sql = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_REMINDER_TIME + " >= ? AND " + COLUMN_REMINDER_TIME + " <= ?" +
                " AND " + COLUMN_IS_DELETED + " = 0" +
                " ORDER BY " + COLUMN_REMINDER_TIME + " ASC";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(startTime), String.valueOf(endTime)});
        if (c.moveToFirst()) {
            do {
                list.add(readNoteFromCursor(c));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // Overloaded method for convenience - converts year, month, day to date string
    public List<Note> getNotesForDate(int year, int month, int day) {
        String dateKey = String.format("%04d-%02d-%02d", year, month + 1, day);
        return getNotesForDate(dateKey);
    }

    public Set<String> getRecurringDates(int startYear, int startMonth, int endYear, int endMonth) {
        Set<String> recurringDates = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT " + COLUMN_REMINDER_TIME + ", " + COLUMN_REPEAT_DAYS +
                " FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_REPEAT_DAYS + " > 0 AND " + COLUMN_IS_DELETED + " = 0";

        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do {
                long reminderTime = c.getLong(0);
                int repeatDays = c.getInt(1);

                if (reminderTime > 0 && repeatDays > 0) {
                    Calendar reminderCal = Calendar.getInstance();
                    reminderCal.setTimeInMillis(reminderTime);

                    Calendar checkDate = Calendar.getInstance();
                    checkDate.set(startYear, startMonth, 1, 0, 0, 0);

                    Calendar endDate = Calendar.getInstance();
                    endDate.set(endYear, endMonth + 1, 1, 0, 0, 0);

                    while (checkDate.before(endDate)) {
                        int dayOfWeek = checkDate.get(Calendar.DAY_OF_WEEK);
                        int bitIndex = convertDayOfWeekToBitIndex(dayOfWeek);

                        if ((repeatDays & (1 << bitIndex)) != 0) {
                            String dateKey = String.format("%04d-%02d-%02d",
                                    checkDate.get(Calendar.YEAR),
                                    checkDate.get(Calendar.MONTH) + 1,
                                    checkDate.get(Calendar.DAY_OF_MONTH));
                            recurringDates.add(dateKey);
                        }
                        checkDate.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        return recurringDates;
    }

    public Set<String> getRecurringDatesForMonth(int year, int month) {
        return getRecurringDates(year, month, year, month);
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

    // ===== STATISTICS =====

    public Map<String, Integer> getCategoryStats() {
        Map<String, Integer> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT " + COLUMN_CATEGORY + ", COUNT(*) FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_DELETED + " = 0" +
                " GROUP BY " + COLUMN_CATEGORY;

        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do {
                String category = c.getString(0);
                int count = c.getInt(1);
                stats.put(category != null ? category : "personal", count);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return stats;
    }

    public Map<String, Integer> getNotesCountForMonth(int year, int month) {
        Map<String, Integer> dateCountMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Calculate start and end of month
        Calendar startOfMonth = Calendar.getInstance();
        startOfMonth.set(year, month, 1, 0, 0, 0);
        startOfMonth.set(Calendar.MILLISECOND, 0);

        Calendar endOfMonth = Calendar.getInstance();
        endOfMonth.set(year, month + 1, 1, 0, 0, 0);
        endOfMonth.add(Calendar.MILLISECOND, -1);

        long startTime = startOfMonth.getTimeInMillis();
        long endTime = endOfMonth.getTimeInMillis();

        // Query notes with reminders in this month
        String sql = "SELECT " + COLUMN_REMINDER_TIME + " FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_REMINDER_TIME + " >= ? AND " + COLUMN_REMINDER_TIME + " <= ?" +
                " AND " + COLUMN_IS_DELETED + " = 0";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(startTime), String.valueOf(endTime)});
        if (c.moveToFirst()) {
            do {
                long reminderTime = c.getLong(0);
                if (reminderTime > 0) {
                    Calendar reminderCal = Calendar.getInstance();
                    reminderCal.setTimeInMillis(reminderTime);

                    String dateKey = String.format("%04d-%02d-%02d",
                            reminderCal.get(Calendar.YEAR),
                            reminderCal.get(Calendar.MONTH) + 1,
                            reminderCal.get(Calendar.DAY_OF_MONTH));

                    dateCountMap.put(dateKey, dateCountMap.getOrDefault(dateKey, 0) + 1);
                }
            } while (c.moveToNext());
        }
        c.close();

        // Also include recurring notes
        Set<String> recurringDates = getRecurringDates(year, month, year, month);
        for (String dateKey : recurringDates) {
            dateCountMap.put(dateKey, dateCountMap.getOrDefault(dateKey, 0) + 1);
        }

        db.close();
        return dateCountMap;
    }

    // ===== INTERNAL HELPER METHODS =====

    private Note readNoteFromCursor(Cursor c) {
        Note n = new Note();
        n.setId(c.getInt(c.getColumnIndexOrThrow(COLUMN_ID)));
        n.setTitle(c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE)));
        n.setContent(c.getString(c.getColumnIndexOrThrow(COLUMN_CONTENT)));
        n.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
        n.setReminderTime(c.getLong(c.getColumnIndexOrThrow(COLUMN_REMINDER_TIME)));
        n.setCompleted(c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)) == 1);
        n.setPinned(c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_PINNED)) == 1);
        n.setRepeatDays(c.getInt(c.getColumnIndexOrThrow(COLUMN_REPEAT_DAYS)));

        // Category
        int idxCat = c.getColumnIndex(COLUMN_CATEGORY);
        n.setCategory(idxCat >= 0 ? c.getString(idxCat) : "personal");

        // Trash fields
        int idxDeleted = c.getColumnIndex(COLUMN_IS_DELETED);
        int idxDeletedAt = c.getColumnIndex(COLUMN_DELETED_AT);
        n.setDeleted(idxDeleted >= 0 ? c.getInt(idxDeleted) == 1 : false);
        n.setDeletedAt(idxDeletedAt >= 0 ? c.getLong(idxDeletedAt) : 0);

        return n;
    }
}