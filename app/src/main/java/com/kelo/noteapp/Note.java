package com.kelo.noteapp;

import java.io.Serializable;

public class Note implements Serializable {
    private int id;
    private String title;
    private String content;
    private long createdAt;
    private long reminderTime;
    private boolean isCompleted;
    private boolean isPinned;

    // Repeat days bitmask (Mon=1<<0 ... Sun=1<<6)
    private int repeatDays;

    // Category (work, personal, family, errand, other)
    private String category;

    // NEW: Trash functionality
    private boolean isDeleted;
    private long deletedAt;

    public Note() {
        this.isCompleted = false;
        this.isPinned = false;
        this.repeatDays = 0;
        this.category = "personal"; // default
        this.isDeleted = false;
        this.deletedAt = 0;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public long getCreatedAt() { return createdAt; }
    public long getReminderTime() { return reminderTime; }
    public boolean isCompleted() { return isCompleted; }
    public boolean isPinned() { return isPinned; }
    public int getRepeatDays() { return repeatDays; }
    public String getCategory() { return category; }
    public boolean isDeleted() { return isDeleted; }
    public long getDeletedAt() { return deletedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setReminderTime(long reminderTime) { this.reminderTime = reminderTime; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    public void setRepeatDays(int repeatDays) { this.repeatDays = repeatDays; }
    public void setCategory(String category) { this.category = category; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public void setDeletedAt(long deletedAt) { this.deletedAt = deletedAt; }

    // Helpers
    public boolean hasReminder() { return reminderTime > 0; }
    public boolean isReminderExpired() { return hasReminder() && reminderTime < System.currentTimeMillis(); }
    public boolean isRepeating() { return repeatDays != 0; }
}