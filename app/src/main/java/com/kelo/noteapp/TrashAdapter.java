package com.kelo.noteapp;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.TrashViewHolder> {

    private final Context context;
    private final List<Note> trashNotes;
    private final OnTrashListener onTrashListener;

    public interface OnTrashListener {
        void onRestoreClick(int position);
        void onPermanentDeleteClick(int position);
    }

    public TrashAdapter(Context context, List<Note> trashNotes, OnTrashListener onTrashListener) {
        this.context = context;
        this.trashNotes = trashNotes;
        this.onTrashListener = onTrashListener;
    }

    @NonNull
    @Override
    public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trash_note, parent, false);
        return new TrashViewHolder(view, onTrashListener);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
        Note note = trashNotes.get(position);

        // Title & content
        holder.textTitle.setText(note.getTitle());
        holder.textContent.setText(note.getContent());

        // Category stripe
        String cat = note.getCategory();
        if (cat != null && !cat.isEmpty()) {
            holder.categoryStripe.setVisibility(View.VISIBLE);
            holder.textCategory.setText(displayNameFor(cat));
            holder.categoryStripe.setBackgroundColor(colorFor(cat));
        } else {
            holder.categoryStripe.setVisibility(View.GONE);
        }

        // Created date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("ru"));
        String dateText = sdf.format(new Date(note.getCreatedAt()));
        holder.textCreatedDate.setText("Создано: " + dateText);

        // Deleted date
        if (note.getDeletedAt() > 0) {
            String deletedText = sdf.format(new Date(note.getDeletedAt()));
            holder.textDeletedDate.setText("Удалено: " + deletedText);
            holder.textDeletedDate.setVisibility(View.VISIBLE);
        } else {
            holder.textDeletedDate.setVisibility(View.GONE);
        }

        // Auto-delete warning (30 days)
        long daysUntilDelete = 30 - ((System.currentTimeMillis() - note.getDeletedAt()) / (24 * 60 * 60 * 1000));
        if (daysUntilDelete > 0) {
            holder.textAutoDelete.setText("Автоудаление через " + daysUntilDelete + " дн.");
            holder.textAutoDelete.setVisibility(View.VISIBLE);
        } else {
            holder.textAutoDelete.setText("Будет удалено автоматически");
            holder.textAutoDelete.setVisibility(View.VISIBLE);
        }

        holder.btnRestore.setOnClickListener(v -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                onTrashListener.onRestoreClick(holder.getAdapterPosition());
            }
        });

        holder.btnPermanentDelete.setOnClickListener(v -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                onTrashListener.onPermanentDeleteClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return trashNotes.size();
    }

    static class TrashViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textTitle, textContent, textCategory;
        TextView textCreatedDate, textDeletedDate, textAutoDelete;
        ImageButton btnRestore, btnPermanentDelete;
        LinearLayout categoryStripe;

        public TrashViewHolder(@NonNull View itemView, OnTrashListener onTrashListener) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textCategory = itemView.findViewById(R.id.textCategory);
            textContent = itemView.findViewById(R.id.textContent);
            textCreatedDate = itemView.findViewById(R.id.textCreatedDate);
            textDeletedDate = itemView.findViewById(R.id.textDeletedDate);
            textAutoDelete = itemView.findViewById(R.id.textAutoDelete);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnPermanentDelete = itemView.findViewById(R.id.btnPermanentDelete);
            categoryStripe = itemView.findViewById(R.id.categoryStripe);
        }
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