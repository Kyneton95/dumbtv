package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * The scrim between the wallpaper and the tiles. A photograph or scene is never evenly lit:
 * the clock needs a darker top-left, the tile labels need a darker bottom, and the corners fall
 * away. Drawing soft gradients instead of one flat dim keeps the picture visible while the text
 * stays readable.
 */
final class Shade extends View {
    private static final int LEFT_EDGE = 0x80000000;
    private static final int TOP_EDGE_NORMAL = 0x66000000;
    private static final int TOP_EDGE_BRIGHT = 0x73000000;
    private static final int CORNER = 0x66000000;
    private static final int[] BOTTOM_NORMAL = {0x00000000, 0x38000000, 0x80000000, 0xB3000000};
    private static final int[] BOTTOM_BRIGHT = {0x00000000, 0x26000000, 0x5C000000, 0x8C000000};
    private static final float[] BOTTOM_STOPS = {0f, 0.42f, 0.72f, 1f};

    private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint();
    private int dim;
    private boolean scenic;
    private boolean bright;

    private int cachedW = -1, cachedH = -1;
    private boolean cachedBright, cachedScenic;
    private LinearGradient left, top, bottom;
    private RadialGradient corners;

    Shade(Context c) {
        super(c);
    }

    void set(int dimPercent, boolean scenic, boolean bright) {
        if (this.dim == dimPercent && this.scenic == scenic && this.bright == bright) return;
        this.dim = dimPercent;
        this.scenic = scenic;
        this.bright = bright;
        invalidate();
    }

    private void ensureGradients(int w, int h) {
        if (w == cachedW && h == cachedH && bright == cachedBright && scenic == cachedScenic) return;
        cachedW = w;
        cachedH = h;
        cachedBright = bright;
        cachedScenic = scenic;

        left = new LinearGradient(0, 0, w * 0.60f, 0, LEFT_EDGE, 0x00000000, Shader.TileMode.CLAMP);
        top = new LinearGradient(0, 0, 0, h * 0.30f,
                bright ? TOP_EDGE_BRIGHT : TOP_EDGE_NORMAL, 0x00000000, Shader.TileMode.CLAMP);
        bottom = new LinearGradient(0, h * 0.34f, 0, h,
                bright ? BOTTOM_BRIGHT : BOTTOM_NORMAL, BOTTOM_STOPS, Shader.TileMode.CLAMP);
        corners = new RadialGradient(w * 0.5f, h * 0.44f, Math.max(w, h) * 0.72f,
                new int[] {0x00000000, 0x00000000, CORNER},
                new float[] {0f, 0.58f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!scenic) return;
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        ensureGradients(w, h);

        gradientPaint.setShader(left);
        canvas.drawRect(0, 0, w * 0.60f, h, gradientPaint);
        gradientPaint.setShader(top);
        canvas.drawRect(0, 0, w, h * 0.30f, gradientPaint);
        gradientPaint.setShader(bottom);
        canvas.drawRect(0, h * 0.34f, w, h, gradientPaint);
        gradientPaint.setShader(corners);
        canvas.drawRect(0, 0, w, h, gradientPaint);

        if (dim > 0) {
            // Separate paint: setShader does not reset Paint alpha, so the dim colour must
            // never be applied to the gradient paint.
            dimPaint.setShader(null);
            dimPaint.setColor(Color.argb(dim * 255 / 100, 0, 0, 0));
            canvas.drawRect(0, 0, w, h, dimPaint);
        }
    }
}
