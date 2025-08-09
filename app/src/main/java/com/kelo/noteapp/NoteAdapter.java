package com.kelo.noteapp;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final Context context;
    private final List<Note> notesList;
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
        holder.textDate.setText(sdf.format(new Date(note.getCreatedAt())));

        // Reminder UI
        if (note.hasReminder()) {
            holder.iconReminder.setVisibility(View.VISIBLE);
            holder.textReminderTime.setVisibility(View.VISIBLE);
            if (note.isReminderExpired()) {
                holder.textReminderTime.setText("Просрочено");
                holder.textReminderTime.setTextColor(context.getColor(android.R.color.holo_red_dark));
            } else {
                SimpleDateFormat rtf = new SimpleDateFormat("dd MMM, HH:mm", new Locale("ru"));
                holder.textReminderTime.setText(rtf.format(new Date(note.getReminderTime())));
                holder.textReminderTime.setTextColor(context.getColor(android.R.color.holo_blue_dark));
            }
        } else {
            holder.iconReminder.setVisibility(View.GONE);
            holder.textReminderTime.setVisibility(View.GONE);
        }

        // Completed styling
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

        // Show/Hide "pinned" badge
        holder.textPinnedBadge.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                onNoteListener.onDeleteClick(holder.getAdapterPosition());
            }
        });

        // Checkbox toggle
        holder.checkboxComplete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
            menu.getMenu().add(0, MENU_PIN, 0, note.isPinned() ? "Открепить" : "Закрепить");
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override public boolean onMenuItemClick(MenuItem item) {
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
        TextView textTitle, textContent, textDate, textReminderTime, textPinnedBadge;
        ImageView iconReminder;
        CheckBox checkboxComplete;
        ImageButton btnDelete;

        public NoteViewHolder(@NonNull View itemView, OnNoteListener onNoteListener) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            textPinnedBadge = itemView.findViewById(R.id.textPinnedBadge);
            textTitle = itemView.findViewById(R.id.textTitle);
            textContent = itemView.findViewById(R.id.textContent);
            textDate = itemView.findViewById(R.id.textDate);
            textReminderTime = itemView.findViewById(R.id.textReminderTime);
            iconReminder = itemView.findViewById(R.id.iconReminder);
            checkboxComplete = itemView.findViewById(R.id.checkboxComplete);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
