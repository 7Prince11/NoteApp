package com.kelo.noteapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final Context context;
    public final List<Note> notesList;
    private final OnNoteListener onNoteListener;

    public interface OnNoteListener {
        void onNoteClick(int position);
        void onDeleteClick(int position);
        void onCompleteClick(int position);
        void onPinClick(int position);
    }

    public NoteAdapter(Context context, List<Note> notesList, OnNoteListener onNoteListener) {
        this.context = context;
        this.notesList = notesList;
        this.onNoteListener = onNoteListener;
    }

    // Add getter method for notesList
    public List<Note> getNotesList() {
        return notesList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view, onNoteListener);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notesList.get(position);

        // Title & content
        holder.textTitle.setText(note.getTitle());
        holder.textContent.setText(note.getContent());

        // Created date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("ru"));
        String dateText = sdf.format(new Date(note.getCreatedAt()));
        holder.textDate.setText(dateText);

        // Category stripe and text
        String cat = note.getCategory();
        if (cat != null && !cat.isEmpty()) {
            holder.categoryStripe.setVisibility(View.VISIBLE);
            holder.textCategory.setText(displayNameFor(cat));
            holder.categoryStripe.setBackgroundColor(colorFor(cat));
        } else {
            holder.categoryStripe.setVisibility(View.GONE);
        }

        // Pin status - using existing badges
        if (note.isPinned()) {
            if (cat != null && !cat.isEmpty()) {
                // Has category - use white badge
                holder.textPinnedBadge.setVisibility(View.VISIBLE);
                holder.textPinnedBadgeAlt.setVisibility(View.GONE);
            } else {
                // No category - use alt badge
                holder.textPinnedBadge.setVisibility(View.GONE);
                holder.textPinnedBadgeAlt.setVisibility(View.VISIBLE);
            }
        } else {
            holder.textPinnedBadge.setVisibility(View.GONE);
            holder.textPinnedBadgeAlt.setVisibility(View.GONE);
        }

        // Reminder handling
        if (note.hasReminder() && !note.isCompleted()) {
            holder.reminderContainer.setVisibility(View.VISIBLE);

            // Format reminder time based on date proximity
            Calendar reminderCal = Calendar.getInstance();
            reminderCal.setTimeInMillis(note.getReminderTime());
            Calendar now = Calendar.getInstance();

            String reminderText;
            if (reminderCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                    reminderCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                // Today - show only time
                SimpleDateFormat timeFormat = new SimpleDateFormat(
                        use24HourFormat() ? "HH:mm" : "h:mma",
                        new Locale("ru")
                );
                reminderText = timeFormat.format(new Date(note.getReminderTime()));
            } else {
                // Other day - show date and time
                SimpleDateFormat compactFormat = new SimpleDateFormat(
                        use24HourFormat() ? "d MMM HH:mm" : "d MMM h:mma",
                        new Locale("ru")
                );
                reminderText = compactFormat.format(new Date(note.getReminderTime()));
            }

            // NEW: For everyday tasks, add repetition info
            if ("everyday".equals(note.getCategory())) {
                reminderText += " (каждые 7 дней)";
            }

            holder.textReminderTime.setText(reminderText);
            holder.textReminderTime.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));

            // Set color based on urgency
            holder.iconReminder.setImageResource(R.drawable.ic_alarm_small);

            // Set color based on urgency
            if (note.isReminderExpired()) {
                // Expired reminder - red color
                holder.iconReminder.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
            } else if (reminderCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                    reminderCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                // Today's reminder - accent color
                holder.iconReminder.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
            } else {
                // Future reminder - primary color
                holder.iconReminder.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
            }
        } else {
            holder.reminderContainer.setVisibility(View.GONE);
        }

        // NEW: Special handling for everyday category
        // For everyday tasks, we can show a special indicator in the category text or modify the display
        if ("everyday".equals(note.getCategory())) {
            // Could add a recurring icon or special text to indicate it's an everyday task
            // Since we don't want to change layout, we can modify the category text display
            holder.textCategory.setText("📅 " + displayNameFor(cat)); // Add emoji to indicate recurring
        }

        // Completion status
        holder.checkboxComplete.setOnCheckedChangeListener(null);
        holder.checkboxComplete.setChecked(note.isCompleted());
        if (note.isCompleted()) {
            holder.textTitle.setPaintFlags(holder.textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.textContent.setPaintFlags(holder.textContent.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.cardView.setAlpha(0.6f);
        } else {
            holder.textTitle.setPaintFlags(holder.textTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.textContent.setPaintFlags(holder.textContent.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.cardView.setAlpha(1.0f);
        }

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                onNoteListener.onDeleteClick(holder.getAdapterPosition());
            }
        });

        // Checkbox toggle
        holder.checkboxComplete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onNoteListener.onCompleteClick(holder.getAdapterPosition());
                }
            }
        });

        // Click → open
        holder.itemView.setOnClickListener(v -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                onNoteListener.onNoteClick(holder.getAdapterPosition());
            }
        });

        // Long‑press menu: Pin / Unpin
        holder.itemView.setOnLongClickListener(v -> {
            PopupMenu menu = new PopupMenu(context, v);
            final int MENU_PIN = 1;
            menu.getMenu().add(0, MENU_PIN, 0, note.isPinned() ?
                    "Открепить" : "Закрепить");
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == MENU_PIN) {
                        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                            onNoteListener.onPinClick(holder.getAdapterPosition());
                            return true;
                        }
                    }
                    return false;
                }
            });
            menu.show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textTitle, textContent, textDate, textReminderTime, textCategory;
        TextView textPinnedBadge, textPinnedBadgeAlt;
        ImageView iconReminder;
        CheckBox checkboxComplete;
        ImageButton btnDelete;
        LinearLayout reminderContainer, categoryStripe;

        public NoteViewHolder(@NonNull View itemView, OnNoteListener onNoteListener) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textCategory = itemView.findViewById(R.id.textCategory);
            textContent = itemView.findViewById(R.id.textContent);
            textDate = itemView.findViewById(R.id.textDate);
            textReminderTime = itemView.findViewById(R.id.textReminderTime);
            iconReminder = itemView.findViewById(R.id.iconReminder);
            checkboxComplete = itemView.findViewById(R.id.checkboxComplete);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            textPinnedBadge = itemView.findViewById(R.id.textPinnedBadge);
            textPinnedBadgeAlt = itemView.findViewById(R.id.textPinnedBadgeAlt);
            reminderContainer = itemView.findViewById(R.id.reminderContainer);
            categoryStripe = itemView.findViewById(R.id.categoryStripe);
        }
    }

    private boolean use24HourFormat() {
        SharedPreferences prefs = context.getSharedPreferences("NotesAppPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("time_24h", true);
    }

    // Category helpers - UPDATED with everyday category
    private String displayNameFor(String key) {
        switch (key) {
            case "work":     return "Работа";
            case "family":   return "Семья";
            case "errand":   return "Поручение";
            case "personal": return "Личное";
            case "everyday": return "Ежедневно"; // NEW
            default:         return "Другое";
        }
    }

    private int colorFor(String key) {
        if ("work".equals(key))     return ContextCompat.getColor(context, R.color.category_work);
        if ("personal".equals(key)) return ContextCompat.getColor(context, R.color.category_personal);
        if ("family".equals(key))   return ContextCompat.getColor(context, R.color.category_family);
        if ("errand".equals(key))   return ContextCompat.getColor(context, R.color.category_errand);
        if ("everyday".equals(key)) return ContextCompat.getColor(context, R.color.recurring_indicator_color); // NEW - Blue color
        return ContextCompat.getColor(context, R.color.category_other);
    }
}