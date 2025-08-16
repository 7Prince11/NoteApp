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
import androidx.core.content.ContextCompat;
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

        // Convert to Monday-based index (Monday = 0, Sunday = 6)
        int startOffset;
        if (firstDayOfWeek == Calendar.SUNDAY) {
            startOffset = 6; // Sunday is last day of week
        } else {
            startOffset = firstDayOfWeek - 2; // Monday = 0, Tuesday = 1, etc.
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

        // Add next month's leading days to fill remaining cells
        Calendar nextMonth = (Calendar) calendar.clone();
        nextMonth.add(Calendar.MONTH, 1);
        int nextMonthNum = nextMonth.get(Calendar.MONTH);
        int nextYear = nextMonth.get(Calendar.YEAR);

        int totalCells = days.size();
        int remainingCells = 42 - totalCells; // 6 rows × 7 days

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

        // Set day number
        holder.dayNumber.setText(String.valueOf(day.day));

        // Set background for today
        if (day.isToday) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.dayNumber.setBackgroundResource(R.drawable.calendar_today_background);
                holder.dayNumber.setTextColor(context.getColor(R.color.text_on_primary));
            } else {
                holder.dayNumber.setBackgroundResource(R.drawable.calendar_today_background);
                holder.dayNumber.setTextColor(context.getResources().getColor(R.color.text_on_primary));
            }
            holder.dayNumber.setTextSize(16);
            holder.dayNumber.setTypeface(null, Typeface.BOLD);
        } else {
            // Reset background
            holder.dayNumber.setBackground(null);

            // Set text color based on month and past/future
            if (!day.isCurrentMonth) {
                // Previous/next month days
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.dayNumber.setTextColor(context.getColor(R.color.text_quaternary));
                } else {
                    holder.dayNumber.setTextColor(context.getResources().getColor(R.color.text_quaternary));
                }
            } else if (day.isPast) {
                // Past days in current month
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.dayNumber.setTextColor(context.getColor(R.color.text_tertiary));
                } else {
                    holder.dayNumber.setTextColor(context.getResources().getColor(R.color.text_tertiary));
                }
            } else {
                // Future days in current month
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.dayNumber.setTextColor(context.getColor(R.color.text_primary));
                } else {
                    holder.dayNumber.setTextColor(context.getResources().getColor(R.color.text_primary));
                }
            }
            holder.dayNumber.setTextSize(14);
            holder.dayNumber.setTypeface(null, Typeface.NORMAL);
        }

        // Show selected state
        if (position == selectedPosition && day.isCurrentMonth && !day.isPast) {
            holder.dayNumber.setBackgroundResource(R.drawable.calendar_selected_background);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.dayNumber.setTextColor(context.getColor(R.color.text_on_primary));
            } else {
                holder.dayNumber.setTextColor(Color.WHITE);
            }
        }

        // Show note indicator - only for current month, future dates (including today)
        String dateKey = day.getDateKey();
        boolean hasNotes = day.isCurrentMonth && !day.isPast && notesCountMap != null &&
                notesCountMap.containsKey(dateKey) && notesCountMap.get(dateKey) > 0;
        boolean hasRecurring = day.isCurrentMonth && !day.isPast && recurringDates != null &&
                recurringDates.contains(dateKey);

        if (hasNotes || hasRecurring) {
            holder.noteIndicator.setVisibility(View.VISIBLE);
            // Use different colors for different types of reminders
            if (hasRecurring && !hasNotes) {
                // Only recurring reminders (daily reminders within 7-day window)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.noteIndicator.setBackgroundTintList(
                            ContextCompat.getColorStateList(context, R.color.recurring_indicator_color));
                } else {
                    holder.noteIndicator.setBackgroundResource(R.drawable.recurring_indicator_dot);
                }
            } else if (hasNotes && !hasRecurring) {
                // Only specific date reminders
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.noteIndicator.setBackgroundTintList(
                            ContextCompat.getColorStateList(context, R.color.note_indicator_color));
                } else {
                    holder.noteIndicator.setBackgroundResource(R.drawable.note_indicator_dot);
                }
            } else {
                // Both types of reminders
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.noteIndicator.setBackgroundTintList(
                            ContextCompat.getColorStateList(context, R.color.mixed_indicator_color));
                } else {
                    holder.noteIndicator.setBackgroundResource(R.drawable.note_indicator_dot);
                }
            }
        } else {
            holder.noteIndicator.setVisibility(View.GONE);
        }

        // Click listener - only for current month and future dates (including today)
        holder.itemView.setOnClickListener(v -> {
            if (day.isCurrentMonth && !day.isPast) {
                int oldSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                // Update both items
                if (oldSelected != -1) {
                    notifyItemChanged(oldSelected);
                }
                notifyItemChanged(selectedPosition);

                // Notify listener
                listener.onDateClick(day.year, day.month, day.day);
            }
        });

        // Enable/disable interaction based on date validity
        holder.itemView.setClickable(day.isCurrentMonth && !day.isPast);
        holder.itemView.setFocusable(day.isCurrentMonth && !day.isPast);

        // Set alpha for visual feedback
        holder.itemView.setAlpha(day.isCurrentMonth && !day.isPast ? 1.0f : 0.6f);
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