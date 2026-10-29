package tv.dumbtv.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Every setting is a row: left/right or OK cycles it, and the change is saved at once. */
public final class Options extends Activity {
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static WeakReference<Options> current;
    private final List<Runnable> refreshers = new ArrayList<>();
    private Config cfg;

    /** Called by Guard when Amazon's HOME event arrives; Home must be the task top. */
    static void dismissForHome() {
        Options o = current == null ? null : current.get();
        if (o != null && !o.isFinishing()) o.finish();
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        current = new WeakReference<>(this);
        cfg = Config.get(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(false);
        GradientDrawable page = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFF0B0C10, 0xFF050608});
        scroll.setBackground(page);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(48), dp(40), dp(48), dp(72));
        scroll.addView(list);

        TextView title = Ui.text(this, 34, Ui.TEXT, Ui.LIGHT);
        title.setText("Options");
        title.setLetterSpacing(-0.01f);
        title.setPadding(dp(18), 0, 0, dp(10));
        list.addView(title);

        TextView intro = Ui.text(this, 15, Ui.FAINT, Ui.REGULAR);
        intro.setText("Changes save as you make them.");
        intro.setPadding(dp(18), 0, 0, dp(12));
        list.addView(intro);

        header(list, "Appearance");
        addChoices(list, Config.ACCENT, Config.TILES, Config.LABELS, Config.CLOCK,
                Config.BACKGROUND, Config.DIM, Config.ROTATE);
        list.addView(action("Next background", () -> {
            if (cfg.nextBackground()) refresh();
        }));

        header(list, "Home screen");
        addChoices(list, Config.LAYOUT, Config.INPUTS, Config.SWITCH);
        list.addView(action("Apps shown", () -> pick(Catalog.APPS)));
        list.addView(action("Inputs shown", () -> pick(Catalog.INPUTS)));
        list.addView(action("Reset order, names and hidden tiles", this::reset));

        header(list, "System");
        list.addView(action("Open Amazon's home screen", () -> {
            Guard.amazon(this);
            finish();
        }));

        TextView about = Ui.text(this, 13, Ui.FAINT, Ui.REGULAR);
        about.setLineSpacing(0, 1.45f);
        about.setPadding(dp(18), dp(30), 0, 0);
        about.setText("Settings: " + cfg.dir + "/" + Config.FILE
                + "\nBuilt-in scenes: Daylight, Aurora, Dunes at dusk and Moonlit coast. Auto: daylight 09:00-17:00, dunes 05:00-09:00 and 17:00-20:00, coast 20:00-00:00, aurora 00:00-05:00."
                + "\nCustom wallpaper: put one wallpaper.jpg (or .png/.webp) beside that file, or several in "
                + Config.WALLPAPERS + "/, then pick Image file or Adaptive wallpaper tint."
                + "\nOn Fire OS 6/7 you can adb push them; Fire OS 8 blocks adb from that folder."
                + "\nNo internet permission. No tracking, no ads, no accounts.");
        list.addView(about);

        setContentView(scroll);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cfg.refresh();
        refresh();
    }

    @Override
    protected void onDestroy() {
        Options o = current == null ? null : current.get();
        if (o == this) current = null;
        super.onDestroy();
    }

    private void refresh() {
        for (Runnable r : refreshers) r.run();
    }

    private void addChoices(LinearLayout list, Config.Choice... choices) {
        for (Config.Choice c : choices) list.addView(choice(c));
    }

    private View choice(Config.Choice c) {
        LinearLayout row = row();
        TextView name = label(c.title);
        TextView value = Ui.text(this, 17, Ui.MUTED, Ui.MEDIUM);
        value.setGravity(Gravity.END);
        row.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));
        row.addView(value, new LinearLayout.LayoutParams(WRAP, WRAP));
        row.setOnClickListener(v -> step(c, 1));
        row.setOnKeyListener((v, code, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (code == KeyEvent.KEYCODE_DPAD_LEFT) return step(c, -1);
            if (code == KeyEvent.KEYCODE_DPAD_RIGHT) return step(c, 1);
            return false;
        });
        Runnable show = () -> {
            boolean focused = row.isFocused();
            String text = c.name(cfg.get(c));
            if (c == Config.ACCENT) {
                SpannableString s = new SpannableString("●  " + text);
                s.setSpan(new ForegroundColorSpan(cfg.accent()), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                value.setText(focused ? "‹  " : "");
                value.append(s);
                if (focused) value.append("  ›");
            } else {
                value.setText(focused ? "‹  " + text + "  ›" : text);
            }
            value.setTextColor(focused ? Ui.TEXT : Ui.MUTED);
            String mode = cfg.get(Config.BACKGROUND);
            boolean image = "image".equals(mode) || "adaptive-tint".equals(mode);
            boolean scenic = cfg.scene() != null || image;
            boolean off = (c == Config.DIM && !scenic)
                    || (c == Config.ROTATE && !image)
                    || (c == Config.SWITCH && "off".equals(cfg.get(Config.INPUTS)));
            row.setAlpha(off ? 0.35f : 1f);
        };
        row.setOnFocusChangeListener((v, has) -> show.run());
        refreshers.add(show);
        refreshers.add(() -> style(row));
        return row;
    }

    private boolean step(Config.Choice c, int by) {
        int i = Arrays.asList(c.values).indexOf(cfg.get(c));
        int n = c.values.length;
        cfg.set(c.key, c.values[((i + by) % n + n) % n]);
        refresh();
        if (c == Config.LOCK && "on".equals(cfg.get(c))) {
            Toast.makeText(this, "Options and Settings are hidden on the home screen now. "
                    + "Hold \u2630 Menu for " + Home.UNLOCK_MS / 1000 + " seconds to come back here.",
                    Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private View action(String text, Runnable run) {
        LinearLayout row = row();
        row.addView(label(text), new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView chevron = Ui.text(this, 18, Ui.MUTED, Ui.REGULAR);
        chevron.setText("›");
        row.addView(chevron);
        row.setOnClickListener(v -> run.run());
        refreshers.add(() -> style(row));
        return row;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setPadding(dp(18), dp(15), dp(18), dp(15));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, WRAP);
        lp.bottomMargin = dp(4);
        row.setLayoutParams(lp);
        return row;
    }

    private void style(View row) {
        int accent = cfg.accent();
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[] {android.R.attr.state_focused},
                Ui.round((accent & 0x00FFFFFF) | 0x2E000000, dp(12),
                        (accent & 0x00FFFFFF) | 0x77000000, dp(1)));
        bg.addState(new int[0], Ui.round(0x00000000, dp(12)));
        row.setBackground(bg);
    }

    private TextView label(String text) {
        TextView v = Ui.text(this, 17, Ui.TEXT, Ui.MEDIUM);
        v.setText(text);
        return v;
    }

    private void header(LinearLayout list, String text) {
        TextView v = Ui.text(this, 13, Ui.FAINT, Ui.MEDIUM);
        v.setText(text.toUpperCase(java.util.Locale.ROOT));
        v.setLetterSpacing(0.18f);
        v.setPadding(dp(18), dp(28), 0, dp(8));
        list.addView(v);
    }

    private void pick(String kind) {
        List<Item> items = Catalog.APPS.equals(kind)
                ? Catalog.apps(this)
                : Catalog.inputs(this, (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE));
        CharSequence[] names = new CharSequence[items.size()];
        boolean[] on = new boolean[items.size()];
        for (int i = 0; i < items.size(); i++) {
            names[i] = Catalog.label(cfg, items.get(i));
            on[i] = Catalog.shown(cfg, kind, items.get(i));
        }
        new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle(Catalog.APPS.equals(kind) ? "Apps on home screen" : "Inputs on home screen")
                .setMultiChoiceItems(names, on, (d, which, checked) ->
                        Catalog.setShown(cfg, kind, items.get(which), checked))
                .setPositiveButton("Done", null)
                .show();
    }

    private void reset() {
        new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle("Reset the home screen layout?")
                .setMessage("Order, names and hidden tiles go back to default. Colors and sizes stay.")
                .setPositiveButton("Reset", (d, w) -> cfg.removePrefix("name.", "apps.", "inputs."))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }
}
