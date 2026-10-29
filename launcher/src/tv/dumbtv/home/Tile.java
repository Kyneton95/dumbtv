package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputManager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/** A card drawn in one pass: banner, or icon plus name, or an input name; focus ring on top. */
final class Tile extends View {
    private static final float FOCUS_SCALE = 1.05f;

    final Item item;
    final String kind;
    private final CharSequence title;
    private final int cardW, cardH, labelH;
    private final boolean labelAlways, labelOnFocus;
    private final float radius, stroke, halo;
    private final RectF card = new RectF();
    private final Path clip = new Path();
    private final Path arrow = new Path();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hairline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotHalo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint inner = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint under = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private boolean moving;
    private final AfterglowTile afterglow;

    Tile(Context c, Item item, String kind, CharSequence title, int cardW, int cardH,
            String labels, int accent) {
        super(c);
        this.item = item;
        this.kind = kind;
        this.title = title;
        this.cardW = cardW;
        this.cardH = cardH;
        boolean app = Catalog.APPS.equals(kind);
        afterglow = app && "afterglow".equals(Config.get(c).get(Config.TILE_STYLE))
                ? new AfterglowTile(c, item.id, title, accent) : null;
        labelH = app && afterglow == null ? Ui.dp(c, 30) : 0;
        labelAlways = "always".equals(labels);
        labelOnFocus = "focus".equals(labels);
        radius = Ui.dp(c, 14);
        stroke = Ui.dp(c, afterglow == null ? 3 : 1f);
        halo = Ui.dp(c, 4);

        fill.setColor(app ? Ui.SURFACE : 0xFF111317);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(stroke);
        ring.setColor(accent);
        ring.setStrokeJoin(Paint.Join.ROUND);
        shadow.setStyle(Paint.Style.STROKE);
        shadow.setStrokeWidth(halo);
        shadow.setColor(0x88000000);
        hairline.setStyle(Paint.Style.STROKE);
        hairline.setStrokeWidth(Math.max(1, Ui.dp(c, 1)));
        hairline.setColor(0x24FFFFFF);
        dot.setColor(0xFF34C759);
        dotHalo.setColor(0xCC000000);

        inner.setColor(Ui.TEXT);
        inner.setTypeface(Ui.MEDIUM);
        inner.setTextAlign(Paint.Align.CENTER);
        inner.setTextSize(Ui.dp(c, app ? 15 : 18));
        under.setColor(0xF2FFFFFF);
        under.setTypeface(Ui.MEDIUM);
        under.setTextAlign(Paint.Align.CENTER);
        under.setTextSize(Ui.dp(c, 16));
        under.setShadowLayer(Ui.dp(c, 3), 0, Ui.dp(c, 1), 0xB3000000);

        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
        setContentDescription(title);
    }

    void setMoving(boolean on) {
        moving = on;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(cardW, cardH + labelH);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        card.set(0, 0, cardW, cardH);
        clip.reset();
        clip.addRoundRect(card, radius, radius, Path.Direction.CW);
        if (afterglow != null) afterglow.resize(cardW, cardH);
        setPivotX(cardW / 2f);
        setPivotY(cardH / 2f);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        float s = focused ? FOCUS_SCALE : 1f;
        animate().scaleX(s).scaleY(s).setDuration(140).start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable banner = item.banner, icon = item.icon;
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawRect(card, fill);
        if (afterglow != null) {
            afterglow.draw(canvas, icon, cardW, cardH);
        } else if (banner != null) {
            banner.setBounds(0, 0, cardW, cardH);
            banner.draw(canvas);
        } else if (icon != null) {
            int size = cardH * 46 / 100;
            int left = (cardW - size) / 2, top = cardH * 12 / 100;
            icon.setBounds(left, top, left + size, top + size);
            icon.draw(canvas);
            drawLine(canvas, inner, cardW / 2f, cardH - cardH * 13 / 100f);
        } else if (!Catalog.APPS.equals(kind)) {
            drawLine(canvas, inner, cardW / 2f, cardH / 2f - (inner.ascent() + inner.descent()) / 2);
        }
        if (moving) canvas.drawColor(0x99000000);
        canvas.drawRoundRect(card, radius, radius, hairline);
        canvas.restore();

        if (afterglow != null) afterglow.edge(canvas, cardW, cardH, radius);

        if (item.state == TvInputManager.INPUT_STATE_CONNECTED) {
            float r = Ui.dp(getContext(), 3.5f);
            canvas.drawCircle(cardW - r * 3.2f, r * 3.2f, r + Ui.dp(getContext(), 1.5f), dotHalo);
            canvas.drawCircle(cardW - r * 3.2f, r * 3.2f, r, dot);
        }
        if (isFocused() || moving) {
            float pad = halo / 2;
            canvas.drawRoundRect(pad, pad, cardW - pad, cardH - pad, radius - pad, radius - pad, shadow);
            float half = stroke / 2;
            ring.setPathEffect(moving ? new DashPathEffect(new float[] {stroke * 3, stroke * 2}, 0) : null);
            canvas.drawRoundRect(half, half, cardW - half, cardH - half, radius - half, radius - half, ring);
        }
        if (moving) drawArrows(canvas);
        boolean label = banner != null && (labelAlways || labelOnFocus && isFocused());
        if (labelH > 0 && label) {
            drawLine(canvas, under, cardW / 2f, cardH + labelH - Ui.dp(getContext(), 7));
        }
    }

    private void drawLine(Canvas canvas, TextPaint paint, float x, float baseline) {
        float room = cardW - Ui.dp(getContext(), 16);
        CharSequence s = TextUtils.ellipsize(title, paint, room, TextUtils.TruncateAt.END);
        canvas.drawText(s, 0, s.length(), x, baseline, paint);
    }

    private void drawArrows(Canvas canvas) {
        float h = cardH / 6f, w = h * 0.7f, mid = cardH / 2f, pad = h * 0.8f;
        inner.setStyle(Paint.Style.FILL);
        arrow.reset();
        arrow.moveTo(pad, mid);
        arrow.lineTo(pad + w, mid - h / 2);
        arrow.lineTo(pad + w, mid + h / 2);
        arrow.close();
        arrow.moveTo(cardW - pad, mid);
        arrow.lineTo(cardW - pad - w, mid - h / 2);
        arrow.lineTo(cardW - pad - w, mid + h / 2);
        arrow.close();
        canvas.drawPath(arrow, inner);
    }
}
