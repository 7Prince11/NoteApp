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

    // Bump version to add "folder" column
    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NAME = "NotesDatabase.db";

    // Table
    private static final String TABLE_NOTES = "notes";

    // Columns (unchanged ones)
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_REMINDER_TIME = "reminder_time";
    private static final String COLUMN_IS_COMPLETED = "is_completed";
    private static final String COLUMN_IS_PINNED = "is_pinned";
    private static final String COLUMN_REPEAT_DAYS = "repeat_days";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_IS_DELETED = "is_deleted";
    private static final String COLUMN_DELETED_AT = "deleted_at";

    // NEW: folder column (independent of category)
    private static final String COLUMN_FOLDER = "folder";

    // Public constants so fragments can use without hardcoding
    public static final String FOLDER_MAIN = "main";
    public static final String FOLDER_SECONDARY = "secondary";

    private static final String CREATE_TABLE_NOTES =
            "CREATE TABLE " + TABLE_NOTES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITLE + " TEXT NOT NULL," +
                    COLUMN_CONTENT + " TEXT," +
                    COLUMN_CREATED_AT + " INTEGER," +
                    COLUMN_REMINDER_TIME + " INTEGER DEFAULT 0," +
                    COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0," +
                    COLUMN_IS_PINNED + " INTEGER DEFAULT 0," +
                    COLUMN_REPEAT_DAYS + " INTEGER DEFAULT 0," +
                    COLUMN_CATEGORY + " TEXT DEFAULT 'personal'," +
                    COLUMN_IS_DELETED + " INTEGER DEFAULT 0," +
                    COLUMN_DELETED_AT + " INTEGER DEFAULT 0," +
                    // NEW: default everything to MAIN folder
                    COLUMN_FOLDER + " TEXT DEFAULT '" + FOLDER_MAIN + "'" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) { db.execSQL(CREATE_TABLE_NOTES); }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_IS_PINNED + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_REPEAT_DAYS + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_CATEGORY + " TEXT DEFAULT 'personal'");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_IS_DELETED + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_DELETED_AT + " INTEGER DEFAULT 0");
        }
        // NEW: add FOLDER and infer from old "category='secondary'" if present
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_FOLDER +
                    " TEXT DEFAULT '" + FOLDER_MAIN + "'");
            // If you previously abused category to store folder, keep category as-is
            // but mark the folder accordingly:
            db.execSQL("UPDATE " + TABLE_NOTES +
                    " SET " + COLUMN_FOLDER + "='" + FOLDER_SECONDARY + "'" +
                    " WHERE " + COLUMN_CATEGORY + "='secondary'");
        }
    }

    // ===== CREATE =====
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
        values.put(COLUMN_IS_DELETED, 0);
        values.put(COLUMN_DELETED_AT, 0);
        // folder defaults to MAIN; if your Note model later adds folder, set it here
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    // ===== READ =====
    public Note getNote(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NOTES, null,
                COLUMN_ID + "=? AND " + COLUMN_IS_DELETED + "=0",
                new String[]{String.valueOf(id)}, null, null, null
        );
        Note note = null;
        if (c != null && c.moveToFirst()) note = readNoteFromCursor(c);
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
            do { list.add(readNoteFromCursor(c)); } while (c.moveToNext());
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
            do { list.add(readNoteFromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // NEW: active notes by folder (main/secondary)
    public List<Note> getActiveNotesByFolder(String folder) {
        if (folder == null) folder = FOLDER_MAIN;
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NOTES, null,
                COLUMN_IS_DELETED + "=0 AND " + COLUMN_IS_COMPLETED + "=0 AND " + COLUMN_FOLDER + "=?",
                new String[]{folder}, null, null,
                COLUMN_IS_PINNED + " DESC, " + COLUMN_CREATED_AT + " DESC");
        if (c.moveToFirst()) {
            do { list.add(readNoteFromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // Existing helper left intact
    public List<Note> getNotesByCategory(String categoryKey) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NOTES, null,
                COLUMN_CATEGORY + "=? AND " + COLUMN_IS_DELETED + "=0 AND " + COLUMN_IS_COMPLETED + "=0",
                new String[]{categoryKey == null ? "personal" : categoryKey},
                null, null,
                COLUMN_IS_PINNED + " DESC, " + COLUMN_CREATED_AT + " DESC");
        if (c.moveToFirst()) {
            do { list.add(readNoteFromCursor(c)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // ===== UPDATE =====
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
        // IMPORTANT: do not touch folder here (so we don’t accidentally reset it)
        int rows = db.update(TABLE_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(note.getId())});
        db.close();
        return rows;
    }

    public void updateNotePinned(int id, boolean pinned) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_IS_PINNED, pinned ? 1 : 0);
        db.update(TABLE_NOTES, v, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Existing method (change visible category only)
    public void updateNoteCategory(int id, String categoryKey) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_CATEGORY, categoryKey == null ? "personal" : categoryKey);
        db.update(TABLE_NOTES, v, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // NEW: move note between tabs without touching category
    public void updateNoteFolder(int id, String folder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_FOLDER, (folder == null ? FOLDER_MAIN : folder));
        db.update(TABLE_NOTES, v, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // ===== TRASH =====
    public void deleteNote(int id) { moveToTrash(id); }

    public void deleteAllNotes() { moveAllToTrash(); }

    public List<Note> getTrashNotes() {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_DELETED + "=1" +
                " ORDER BY " + COLUMN_DELETED_AT + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do { list.add(readNoteFromCursor(c)); } while (c.moveToNext());
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

    public void moveAllToTrash() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DELETED, 1);
        values.put(COLUMN_DELETED_AT, System.currentTimeMillis());
        db.update(TABLE_NOTES, values, COLUMN_IS_DELETED + " = 0", null);
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

    public int getTrashCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + TABLE_NOTES + " WHERE " + COLUMN_IS_DELETED + " = 1";
        Cursor c = db.rawQuery(sql, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    public int cleanupOldTrashNotes(int daysOld) {
        SQLiteDatabase db = this.getWritableDatabase();
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
        int deletedCount = db.delete(TABLE_NOTES,
                COLUMN_IS_DELETED + " = 1 AND " + COLUMN_DELETED_AT + " < ?",
                new String[]{String.valueOf(cutoffTime)});
        db.close();
        return deletedCount;
    }

    // ===== CALENDAR (same as before) =====
    public Map<String, Integer> getNotesCountForMonth(int year, int month) {
        Map<String, Integer> dateCountMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long monthStart = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        long monthEnd = calendar.getTimeInMillis();

        String sql = "SELECT " + COLUMN_REMINDER_TIME + " FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_DELETED + "=0" +
                " AND " + COLUMN_IS_COMPLETED + "=0" +
                " AND " + COLUMN_REMINDER_TIME + " BETWEEN ? AND ?" +
                " AND " + COLUMN_CATEGORY + " != 'everyday'";

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(monthStart), String.valueOf(monthEnd) });
        if (c.moveToFirst()) {
            do {
                long reminderTime = c.getLong(0);
                if (reminderTime > 0) {
                    Calendar reminderDate = Calendar.getInstance();
                    reminderDate.setTimeInMillis(reminderTime);
                    String dateKey = String.format("%04d-%02d-%02d",
                            reminderDate.get(Calendar.YEAR),
                            reminderDate.get(Calendar.MONTH) + 1,
                            reminderDate.get(Calendar.DAY_OF_MONTH));
                    dateCountMap.put(dateKey, dateCountMap.getOrDefault(dateKey, 0) + 1);
                }
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return dateCountMap;
    }

    public List<Note> getNotesForDate(int year, int month, int day) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Calendar dayStart = Calendar.getInstance();
        dayStart.set(year, month, day, 0, 0, 0);
        dayStart.set(Calendar.MILLISECOND, 0);
        long startTime = dayStart.getTimeInMillis();

        Calendar dayEnd = Calendar.getInstance();
        dayEnd.set(year, month, day, 23, 59, 59);
        dayEnd.set(Calendar.MILLISECOND, 999);

        String sql = "SELECT * FROM " + TABLE_NOTES +
                " WHERE " + COLUMN_IS_DELETED + "=0" +
                " AND " + COLUMN_IS_COMPLETED + "=0" +
                " AND " + COLUMN_REMINDER_TIME + " BETWEEN ? AND ?" +
                " AND " + COLUMN_CATEGORY + " != 'everyday'" +
                " ORDER BY " + COLUMN_REMINDER_TIME + " ASC";

        Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(startTime), String.valueOf(dayEnd.getTimeInMillis()) });
        if (c.moveToFirst()) {
            do { notes.add(readNoteFromCursor(c)); } while (c.moveToNext());
        }
        c.close();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        Calendar sevenDaysEnd = (Calendar) today.clone();
        sevenDaysEnd.add(Calendar.DAY_OF_MONTH, 6);

        if (!dayStart.before(today) && !dayStart.after(sevenDaysEnd)) {
            int weekdayIndex = convertDayOfWeekToBitIndex(dayStart.get(Calendar.DAY_OF_WEEK));
            int mask = (1 << weekdayIndex);

            String everySql = "SELECT * FROM " + TABLE_NOTES +
                    " WHERE " + COLUMN_IS_DELETED + "=0" +
                    " AND " + COLUMN_IS_COMPLETED + "=0" +
                    " AND " + COLUMN_CATEGORY + "='everyday' " +
                    " AND " + COLUMN_REPEAT_DAYS + " > 0 " +
                    " AND ((" + COLUMN_REPEAT_DAYS + " & ?) != 0)";

            Cursor ec = db.rawQuery(everySql, new String[]{ String.valueOf(mask) });
            if (ec.moveToFirst()) {
                do { notes.add(readNoteFromCursor(ec)); } while (ec.moveToNext());
            }
            ec.close();
        }

        db.close();
        return notes;
    }

    public Set<String> getRecurringDates(int startYear, int startMonth, int endYear, int endMonth) {
        Set<String> recurringDates = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 7; i++) {
            Calendar check = (Calendar) today.clone();
            check.add(Calendar.DAY_OF_MONTH, i);

            int weekdayIndex = convertDayOfWeekToBitIndex(check.get(Calendar.DAY_OF_WEEK));
            int mask = (1 << weekdayIndex);

            String sql = "SELECT 1 FROM " + TABLE_NOTES +
                    " WHERE " + COLUMN_IS_DELETED + "=0" +
                    " AND " + COLUMN_IS_COMPLETED + "=0" +
                    " AND " + COLUMN_CATEGORY + "='everyday' " +
                    " AND " + COLUMN_REPEAT_DAYS + " > 0 " +
                    " AND ((" + COLUMN_REPEAT_DAYS + " & ?) != 0) LIMIT 1";

            Cursor c = db.rawQuery(sql, new String[]{ String.valueOf(mask) });
            boolean exists = c.moveToFirst();
            c.close();

            if (exists) {
                String dateKey = String.format("%04d-%02d-%02d",
                        check.get(Calendar.YEAR),
                        check.get(Calendar.MONTH) + 1,
                        check.get(Calendar.DAY_OF_MONTH));
                recurringDates.add(dateKey);
            }
        }

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

    // ===== INTERNAL =====
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

        int idxCat = c.getColumnIndex(COLUMN_CATEGORY);
        n.setCategory(idxCat >= 0 ? c.getString(idxCat) : "personal");

        int idxDeleted = c.getColumnIndex(COLUMN_IS_DELETED);
        int idxDeletedAt = c.getColumnIndex(COLUMN_DELETED_AT);
        n.setDeleted(idxDeleted >= 0 && c.getInt(idxDeleted) == 1);
        n.setDeletedAt(idxDeletedAt >= 0 ? c.getLong(idxDeletedAt) : 0);

        // Note: we do not need to store folder on Note model for UI,
        // because folder is only used to filter/move in DB. (Optional to add later.)
        return n;
    }
}
