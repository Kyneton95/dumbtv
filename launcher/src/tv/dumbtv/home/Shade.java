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
 * away. Drawing three soft gradients instead of one flat dim keeps the picture visible while
 * every piece of text stays above 7:1.
 */
final class Shade extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int dim;
    private boolean scenic;

    Shade(Context c) {
        super(c);
    }

    void set(int dimPercent, boolean scenic) {
        if (this.dim == dimPercent && this.scenic == scenic) return;
        this.dim = dimPercent;
        this.scenic = scenic;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!scenic) return;
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        // left: holds the clock and the row titles
        paint.setShader(new LinearGradient(0, 0, w * 0.60f, 0, 0x80000000, 0x00000000,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w * 0.60f, h, paint);

        // top: keeps the chips readable if the sky is bright
        paint.setShader(new LinearGradient(0, 0, 0, h * 0.30f, 0x66000000, 0x00000000,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h * 0.30f, paint);

        // bottom: the labels sit on this, and a mid stop stops a bright horizon from
        // cutting through them
        paint.setShader(new LinearGradient(0, h * 0.34f, 0, h,
                new int[] {0x00000000, 0x38000000, 0x80000000, 0xB3000000},
                new float[] {0f, 0.42f, 0.72f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, h * 0.34f, w, h, paint);

        // corners fall away, so the eye stays on the rows
        paint.setShader(new RadialGradient(w * 0.5f, h * 0.44f, Math.max(w, h) * 0.72f,
                new int[] {0x00000000, 0x00000000, 0x66000000},
                new float[] {0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        if (dim > 0) {
            paint.setShader(null);
            paint.setColor(Color.argb(dim * 255 / 100, 0, 0, 0));
            canvas.drawRect(0, 0, w, h, paint);
        }
    }
}
