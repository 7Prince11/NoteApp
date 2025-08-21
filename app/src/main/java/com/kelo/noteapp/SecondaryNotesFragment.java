package com.kelo.noteapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class SecondaryNotesFragment extends Fragment {

    private RecyclerView recycler;
    private View emptyText;
    private NoteAdapter adapter;
    private final List<Note> data = new ArrayList<>();
    private DatabaseHelper db;
    private boolean suppressSwipe = false;

    public static SecondaryNotesFragment newInstance() { return new SecondaryNotesFragment(); }

    @Override public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        db = new DatabaseHelper(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notes_list, container, false);
        recycler = v.findViewById(R.id.notesRecycler);
        emptyText = v.findViewById(R.id.emptyText);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NoteAdapter(getContext(), data, new NoteAdapter.OnNoteListener() {
            @Override public void onNoteClick(int position) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openEditFromNote(data.get(position));
                }
            }

            @Override public void onDeleteClick(int position) {
                Note note = data.get(position);
                db.moveToTrash(note.getId());
                data.remove(position);
                adapter.notifyItemRemoved(position);
                updateEmpty();
                Snackbar.make(recycler, "Заметка перемещена в корзину", Snackbar.LENGTH_LONG)
                        .setAction("ОТМЕНИТЬ", v1 -> {
                            db.restoreFromTrash(note.getId());
                            data.add(position, note);
                            adapter.notifyItemInserted(position);
                            updateEmpty();
                        }).show();
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).notifyTabsChanged();
            }

            @Override public void onCompleteClick(int position) {
                Note note = data.get(position);
                note.setCompleted(!note.isCompleted());
                db.updateNote(note);
                adapter.notifyItemChanged(position);
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).handleCompleteToggle(note);
            }

            @Override public void onPinClick(int position) {
                Note note = data.get(position);
                note.setPinned(!note.isPinned());
                db.updateNote(note);
                sortDefault();
                adapter.notifyDataSetChanged();
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).notifyTabsChanged();
            }

            @Override public void onMoveToSecondary(int position) {
                // here "move" means to PRIMARY
                Note note = data.get(position);
                db.updateNoteCategory(note.getId(), "personal");
                data.remove(position);
                adapter.notifyItemRemoved(position);
                updateEmpty();
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).notifyTabsChanged();
            }
        });
        recycler.setAdapter(adapter);

        attachSwipeToDelete();
        attachLongPressForActions(true); // secondary list

        reload();
        return v;
    }

    private void attachLongPressForActions(boolean isSecondaryList) {
        GestureDetector detector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent e) { return true; }
                    @Override public void onLongPress(MotionEvent e) {
                        View child = recycler.findChildViewUnder(e.getX(), e.getY());
                        if (child == null) return;
                        int pos = recycler.getChildAdapterPosition(child);
                        if (pos == RecyclerView.NO_POSITION || pos >= data.size()) return;
                        suppressSwipe = true;
                        showActionsSheet(data.get(pos), pos, isSecondaryList);
                    }
                });

        recycler.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                detector.onTouchEvent(e);
                return false;
            }
            @Override public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}
            @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
    }

    private void showActionsSheet(Note note, int pos, boolean isSecondaryList) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(getContext())
                .inflate(R.layout.sheet_note_actions, null, false);

        TextView actionPin  = sheet.findViewById(R.id.actionPin);
        TextView actionMove = sheet.findViewById(R.id.actionMove);

        actionPin.setText(note.isPinned() ? "Открепить" : "Закрепить");
        actionMove.setText(isSecondaryList ? "Переместить в основную" : "Переместить в доп. папку");

        actionPin.setOnClickListener(v -> {
            note.setPinned(!note.isPinned());
            db.updateNote(note);
            sortDefault();
            adapter.notifyDataSetChanged();
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).notifyTabsChanged();
            dialog.dismiss();
        });

        actionMove.setOnClickListener(v -> {
            if (isSecondaryList) {
                db.updateNoteCategory(note.getId(), "personal");
                data.remove(pos);
                adapter.notifyItemRemoved(pos);
            } else {
                db.updateNoteCategory(note.getId(), "secondary");
                data.remove(pos);
                adapter.notifyItemRemoved(pos);
            }
            updateEmpty();
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).notifyTabsChanged();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> suppressSwipe = false);
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void attachSwipeToDelete() {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override public boolean isItemViewSwipeEnabled() { return !suppressSwipe; }

            @Override public boolean onMove(@NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos >= 0 && pos < data.size()) {
                    Note note = data.get(pos);
                    db.moveToTrash(note.getId());
                    data.remove(pos);
                    adapter.notifyItemRemoved(pos);
                    updateEmpty();
                    Snackbar.make(recycler, "Заметка перемещена в корзину", Snackbar.LENGTH_LONG)
                            .setAction("ОТМЕНИТЬ", v -> {
                                db.restoreFromTrash(note.getId());
                                data.add(pos, note);
                                adapter.notifyItemInserted(pos);
                                updateEmpty();
                            }).show();
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).notifyTabsChanged();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View item = viewHolder.itemView;
                    Paint p = new Paint();
                    p.setColor(ContextCompat.getColor(requireContext(), R.color.delete_color));
                    if (dX > 0) c.drawRect(item.getLeft(), item.getTop(), dX, item.getBottom(), p);
                    else c.drawRect(item.getRight() + dX, item.getTop(), item.getRight(), item.getBottom(), p);

                    Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
                    if (icon != null) {
                        int m = (item.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int top = item.getTop() + m;
                        int bottom = top + icon.getIntrinsicHeight();
                        if (dX > 0) {
                            int left = item.getLeft() + m;
                            int right = left + icon.getIntrinsicWidth();
                            icon.setBounds(left, top, right, bottom);
                        } else {
                            int right = item.getRight() - m;
                            int left = right - icon.getIntrinsicWidth();
                            icon.setBounds(left, top, right, bottom);
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) icon.setTint(Color.WHITE);
                        icon.draw(c);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        helper.attachToRecyclerView(recycler);
    }

    private void sortDefault() {
        Collections.sort(data, new Comparator<Note>() {
            @Override public int compare(Note a, Note b) {
                if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
        });
    }

    private void updateEmpty() {
        if (emptyText != null) emptyText.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public void reload() {
        if (!isAdded()) return;
        if (db == null) db = new DatabaseHelper(requireContext());
        data.clear();
        List<Note> all = db.getActiveNotes();
        for (Note n : all) {
            String cat = n.getCategory() == null ? "personal" : n.getCategory();
            if ("secondary".equalsIgnoreCase(cat)) data.add(n);
        }
        sortDefault();
        if (adapter != null) adapter.notifyDataSetChanged();
        updateEmpty();
    }
}
