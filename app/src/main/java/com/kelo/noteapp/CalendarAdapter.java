// app/src/main/java/com/kelo/noteapp/CalendarAdapter.java
package com.kelo.noteapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
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

    public interface OnDateClickListener {
        void onDateClick(int year, int month, int day);
    }

    private final Context context;
    private final OnDateClickListener listener;
    private final List<CalendarDay> days;
    private final Calendar todayCalendar;
    private int selectedPosition = -1;
    private Map<String, Integer> notesCountMap;
    private Set<String> recurringDates;

    static class CalendarDay {
        int day;
        int month;
        int year;
        boolean isCurrentMonth;
        boolean isToday;
        boolean isTomorrow;
        boolean isPast;

        CalendarDay(int day, int month, int year, boolean isCurrentMonth, boolean isToday, boolean isTomorrow, boolean isPast) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.isTomorrow = isTomorrow;
            this.isPast = isPast;
        }

        public String getDateKey() {
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
        this.notesCountMap = notesCountMap;   // specific dated notes (orange)
        this.recurringDates = recurringDates; // ONLY everyday+repeat within next 7 days
        days.clear();
        selectedPosition = -1;

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);

        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);

        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        int tomorrowDay = tomorrow.get(Calendar.DAY_OF_MONTH);
        int tomorrowMonth = tomorrow.get(Calendar.MONTH);
        int tomorrowYear = tomorrow.get(Calendar.YEAR);

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int mondayBasedFirst = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : (firstDayOfWeek - 2);

        Calendar prevMonth = (Calendar) calendar.clone();
        prevMonth.add(Calendar.MONTH, -1);
        int prevMonthNum = prevMonth.get(Calendar.MONTH);
        int prevYear = prevMonth.get(Calendar.YEAR);
        int prevMonthDays = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = mondayBasedFirst - 1; i >= 0; i--) {
            int dayNum = prevMonthDays - i;
            Calendar dayCheck = Calendar.getInstance();
            dayCheck.set(prevYear, prevMonthNum, dayNum, 0, 0, 0);
            dayCheck.set(Calendar.MILLISECOND, 0);
            boolean isPast = dayCheck.before(today);
            days.add(new CalendarDay(dayNum, prevMonthNum, prevYear, false, false, false, isPast));
        }

        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= daysInMonth; d++) {
            boolean isToday = (d == todayDay && month == todayMonth && year == todayYear);
            boolean isTomorrow = (d == tomorrowDay && month == tomorrowMonth && year == tomorrowYear);

            Calendar dayCheck = Calendar.getInstance();
            dayCheck.set(year, month, d, 0, 0, 0);
            dayCheck.set(Calendar.MILLISECOND, 0);
            boolean isPast = dayCheck.before(today);

            days.add(new CalendarDay(d, month, year, true, isToday, isTomorrow, isPast));
        }

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
            days.add(new CalendarDay(i, nextMonthNum, nextYear, false, false, false, isPast));
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

        if (!day.isCurrentMonth) {
            holder.dayNumber.setAlpha(0.3f);
        } else if (day.isPast) {
            holder.dayNumber.setAlpha(0.5f);
        } else {
            holder.dayNumber.setAlpha(1.0f);
        }

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
        holder.dayNumber.setTypeface(null, Typeface.NORMAL);

        holder.dayNumber.setSelected(position == selectedPosition);
        if (position == selectedPosition && !day.isPast) {
            holder.dayNumber.setTextColor(Color.WHITE);
        }

        String dateKey = day.getDateKey();
        boolean hasSpecificNotes = !day.isPast && notesCountMap != null &&
                notesCountMap.containsKey(dateKey) && notesCountMap.get(dateKey) > 0;

        // BLUE dot now comes ONLY from recurringDates, which is limited to everyday+repeat within 7 days
        boolean hasEverydayTasks = !day.isPast && recurringDates != null &&
                recurringDates.contains(dateKey);

        if (hasSpecificNotes || hasEverydayTasks) {
            holder.noteIndicator.setVisibility(View.VISIBLE);
            if (hasSpecificNotes) {
                holder.noteIndicator.setBackgroundResource(R.drawable.note_indicator_dot); // orange
            } else {
                holder.noteIndicator.setBackgroundResource(R.drawable.recurring_indicator_dot); // blue
            }
        } else {
            holder.noteIndicator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (day.isCurrentMonth && !day.isPast) {
                int oldSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(oldSelected);
                notifyItemChanged(selectedPosition);
                listener.onDateClick(day.year, day.month, day.day);
            }
        });

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
