package com.kelo.noteapp;


import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private Context context;
    private List<Note> notesList;
    private OnNoteListener onNoteListener;

    public interface OnNoteListener {
        void onNoteClick(int position);
        void onDeleteClick(int position);
        void onCompleteClick(int position);
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

        // Установка заголовка и содержания
        holder.textTitle.setText(note.getTitle());
        holder.textContent.setText(note.getContent());

        // Форматирование даты создания
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("ru"));
        String dateCreated = sdf.format(new Date(note.getCreatedAt()));
        holder.textDate.setText(dateCreated);

        // Отображение напоминания
        if (note.hasReminder()) {
            holder.iconReminder.setVisibility(View.VISIBLE);

            if (note.isReminderExpired()) {
                holder.textReminderTime.setText("Напоминание прошло");
                holder.textReminderTime.setTextColor(context.getColor(android.R.color.holo_red_dark));
            } else {
                SimpleDateFormat reminderSdf = new SimpleDateFormat("dd MMM, HH:mm", new Locale("ru"));
                String reminderTime = reminderSdf.format(new Date(note.getReminderTime()));
                holder.textReminderTime.setText(reminderTime);
                holder.textReminderTime.setTextColor(context.getColor(android.R.color.holo_blue_dark));
            }
            holder.textReminderTime.setVisibility(View.VISIBLE);
        } else {
            holder.iconReminder.setVisibility(View.GONE);
            holder.textReminderTime.setVisibility(View.GONE);
        }

        // Установка состояния выполнения
        holder.checkboxComplete.setOnCheckedChangeListener(null);
        holder.checkboxComplete.setChecked(note.isCompleted());

        // Применение стиля для выполненных задач
        if (note.isCompleted()) {
            holder.textTitle.setPaintFlags(holder.textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.textContent.setPaintFlags(holder.textContent.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.cardView.setAlpha(0.6f);
        } else {
            holder.textTitle.setPaintFlags(holder.textTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.textContent.setPaintFlags(holder.textContent.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.cardView.setAlpha(1.0f);
        }

        // Обработчик изменения состояния выполнения
        holder.checkboxComplete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onNoteListener.onCompleteClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public class NoteViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        CardView cardView;
        TextView textTitle, textContent, textDate, textReminderTime;
        ImageView iconReminder;
        ImageButton btnDelete;
        CheckBox checkboxComplete;
        OnNoteListener onNoteListener;

        public NoteViewHolder(@NonNull View itemView, OnNoteListener onNoteListener) {
            super(itemView);

            cardView = itemView.findViewById(R.id.cardView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textContent = itemView.findViewById(R.id.textContent);
            textDate = itemView.findViewById(R.id.textDate);
            textReminderTime = itemView.findViewById(R.id.textReminderTime);
            iconReminder = itemView.findViewById(R.id.iconReminder);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            checkboxComplete = itemView.findViewById(R.id.checkboxComplete);

            this.onNoteListener = onNoteListener;

            itemView.setOnClickListener(this);

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNoteListener.onDeleteClick(getAdapterPosition());
                }
            });
        }

        @Override
        public void onClick(View v) {
            onNoteListener.onNoteClick(getAdapterPosition());
        }
    }
}