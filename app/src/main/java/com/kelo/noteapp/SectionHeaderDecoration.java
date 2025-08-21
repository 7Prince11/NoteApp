package com.kelo.noteapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Простой ItemDecoration для секций:
 * - "Главное" над первой закреплённой
 * - "Второй приоритет" над первой незакреплённой
 */
public class SectionHeaderDecoration extends RecyclerView.ItemDecoration {

    private final Context context;
    private final List<Note> data; // список в порядке отображения: сначала pinned, потом остальные
    private final Paint bgPaint;
    private final Paint textPaint;
    private final int headerHeight;
    private final int paddingH;
    private final int textBaseline;

    public SectionHeaderDecoration(Context ctx, List<Note> data) {
        this.context = ctx.getApplicationContext();
        this.data = data;

        headerHeight = dp(32);
        paddingH = dp(12);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(context.getResources().getColor(R.color.chip_background)); // мягкая подложка

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(context.getResources().getColor(R.color.text_secondary));
        textPaint.setTextSize(sp(13));

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        textBaseline = (int) ((headerHeight - fm.bottom + fm.top) / 2f - fm.top);
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (data == null || data.isEmpty()) return;

        // ищем первую pinned и первую unpinned позиции
        int firstPinnedPos = -1;
        int firstUnpinnedPos = -1;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).isPinned()) { firstPinnedPos = i; break; }
        }
        for (int i = 0; i < data.size(); i++) {
            if (!data.get(i).isPinned()) { firstUnpinnedPos = i; break; }
        }

        // рисуем, если элемент видим
        drawHeaderForPosition(c, parent, firstPinnedPos, "Главное");
        drawHeaderForPosition(c, parent, firstUnpinnedPos, "Второй приоритет");
    }

    private void drawHeaderForPosition(Canvas canvas, RecyclerView rv, int adapterPos, String title) {
        if (adapterPos < 0) return;
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(adapterPos);
        if (vh == null) return;
        View item = vh.itemView;

        int left = rv.getPaddingLeft();
        int right = rv.getWidth() - rv.getPaddingRight();
        int top = item.getTop() - headerHeight;
        int bottom = item.getTop();

        // не залезаем за границы
        if (bottom <= rv.getPaddingTop()) return;

        // фон
        canvas.drawRect(left, Math.max(top, rv.getPaddingTop()), right, bottom, bgPaint);
        // текст
        canvas.drawText(title, left + paddingH, bottom - (headerHeight - textBaseline), textPaint);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION || data == null || data.isEmpty()) return;

        boolean isFirstPinned = data.get(position).isPinned()
                && (position == 0 || !data.get(position - 1).isPinned());
        boolean isFirstUnpinned = !data.get(position).isPinned()
                && (position == 0 || data.get(position - 1).isPinned());

        if (isFirstPinned || isFirstUnpinned) {
            outRect.top += headerHeight;
        }
    }

    private int dp(int v) {
        float d = context.getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    private float sp(int v) {
        float s = context.getResources().getDisplayMetrics().scaledDensity;
        return v * s;
    }
}
