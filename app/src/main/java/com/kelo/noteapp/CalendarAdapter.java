package com.kelo.noteapp;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final Context context;
    private final List<CalendarDay> days;
    private final OnDateClickListener listener;
    private int selectedPosition = -1;
    private Map<String, Integer> notesCountMap;
    private Set<String> recurringDates;
    private Calendar todayCalendar;

    public interface OnDateClickListener {
        void onDateClick(int year, int month, int day);
    }

    public static class CalendarDay {
        public final int day;
        public final int month;
        public final int year;
        public final boolean isCurrentMonth;
        public final boolean isToday;
        public final boolean isPast;

        public CalendarDay(int day, int month, int year, boolean isCurrentMonth, boolean isToday, boolean isPast) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.isPast = isPast;
        }

        public String getDateKey() {
            // Month is 0-based in Calendar, so add 1 for display
            return String.format("%04d-%02d-%02d", year, month + 1, day);
        }
    }

    public CalendarAdapter(Context context, OnDateClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.days = new ArrayList<>();
        this.todayCalendar = Calendar.getInstance();
    }

    public void setMonth(int year, int month, Map<String, Integer> notesCountMap, Set<String> recurringDates) {
        this.notesCountMap = notesCountMap;
        this.recurringDates = recurringDates;
        days.clear();
        selectedPosition = -1;

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);

        // Get today's date for comparison
        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);

        // Set today to start of day for comparison
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // Get first day of month
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // Convert to Monday-based index
        int startOffset;
        if (firstDayOfWeek == Calendar.SUNDAY) {
            startOffset = 6;
        } else {
            startOffset = firstDayOfWeek - 2;
        }

        // Add previous month's trailing days
        if (startOffset > 0) {
            Calendar prevMonth = (Calendar) calendar.clone();
            prevMonth.add(Calendar.MONTH, -1);
            int prevMonthDays = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
            int prevMonthNum = prevMonth.get(Calendar.MONTH);
            int prevYear = prevMonth.get(Calendar.YEAR);

            for (int i = prevMonthDays - startOffset + 1; i <= prevMonthDays; i++) {
                Calendar dayCheck = Calendar.getInstance();
                dayCheck.set(prevYear, prevMonthNum, i, 0, 0, 0);
                dayCheck.set(Calendar.MILLISECOND, 0);
                boolean isPast = dayCheck.before(today);
                days.add(new CalendarDay(i, prevMonthNum, prevYear, false, false, isPast));
            }
        }

        // Add current month's days
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            boolean isToday = (i == todayDay && month == todayMonth && year == todayYear);
            Calendar dayCheck = Calendar.getInstance();
            dayCheck.set(year, month, i, 0, 0, 0);
            dayCheck.set(Calendar.MILLISECOND, 0);
            boolean isPast = dayCheck.before(today) && !isToday;
            days.add(new CalendarDay(i, month, year, true, isToday, isPast));
        }

        // Add next month's leading days
        Calendar nextMonth = (Calendar) calendar.clone();
        nextMonth.add(Calendar.MONTH, 1);
        int nextMonthNum = nextMonth.get(Calendar.MONTH);
        int nextYear = nextMonth.get(Calendar.YEAR);

        int totalCells = days.size();
        int remainingCells = 42 - totalCells;

        for (int i = 1; i <= remainingCells; i++) {
            Calendar dayCheck = Calendar.getInstance();
            dayCheck.set(nextYear, nextMonthNum, i, 0, 0, 0);
            dayCheck.set(Calendar.MILLISECOND, 0);
            boolean isPast = dayCheck.before(today);
            days.add(new CalendarDay(i, nextMonthNum, nextYear, false, false, isPast));
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay day = days.get(position);

        holder.dayNumber.setText(String.valueOf(day.day));

        // Style based on current month and past/future
        if (!day.isCurrentMonth) {
            holder.dayNumber.setAlpha(0.3f);
        } else if (day.isPast) {
            holder.dayNumber.setAlpha(0.5f); // Dim past dates
        } else {
            holder.dayNumber.setAlpha(1.0f);
        }

        // Highlight today
        if (day.isToday) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.dayNumber.setTextColor(context.getColor(R.color.colorAccent));
            } else {
                holder.dayNumber.setTextColor(context.getResources().getColor(R.color.colorAccent));
            }
            holder.dayNumber.setTextSize(16);
            holder.dayNumber.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.dayNumber.setTextColor(day.isPast ?
                        context.getColor(R.color.text_tertiary) :
                        context.getColor(R.color.text_primary));
            } else {
                holder.dayNumber.setTextColor(day.isPast ?
                        context.getResources().getColor(R.color.text_tertiary) :
                        context.getResources().getColor(R.color.text_primary));
            }
            holder.dayNumber.setTextSize(14);
            holder.dayNumber.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Show selected state
        holder.dayNumber.setSelected(position == selectedPosition);
        if (position == selectedPosition && !day.isPast) {
            holder.dayNumber.setTextColor(Color.WHITE);
        }

        // Show note indicator ONLY for future dates
        String dateKey = day.getDateKey();
        boolean hasNotes = !day.isPast && notesCountMap != null &&
                notesCountMap.containsKey(dateKey) && notesCountMap.get(dateKey) > 0;
        boolean hasRecurring = !day.isPast && recurringDates != null &&
                recurringDates.contains(dateKey);

        if (hasNotes || hasRecurring) {
            holder.noteIndicator.setVisibility(View.VISIBLE);
            // Use different color for recurring
            if (hasRecurring && !hasNotes) {
                holder.noteIndicator.setBackgroundResource(R.drawable.recurring_indicator_dot);
            } else {
                holder.noteIndicator.setBackgroundResource(R.drawable.note_indicator_dot);
            }
        } else {
            holder.noteIndicator.setVisibility(View.GONE);
        }

        // Click listener - only for current month and future dates
        holder.itemView.setOnClickListener(v -> {
            if (day.isCurrentMonth && !day.isPast) {
                int oldSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(oldSelected);
                notifyItemChanged(selectedPosition);
                listener.onDateClick(day.year, day.month, day.day);
            }
        });

        // Disable click for past dates
        holder.itemView.setClickable(!day.isPast);
        holder.itemView.setFocusable(!day.isPast);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView dayNumber;
        View noteIndicator;

        CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.dayNumber);
            noteIndicator = itemView.findViewById(R.id.noteIndicator);
        }
    }
}