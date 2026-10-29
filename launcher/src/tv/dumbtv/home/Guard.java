package tv.dumbtv.home;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS keeps HOME bound to Amazon's protected launcher and handles the HOME key before any
 * accessibility filter sees it, so this service waits for Amazon's home window and replaces it.
 *
 * A plain startActivity right after a HOME press is held back by Android's five-second
 * app-switch delay. An alarm-fired PendingIntent is sent by system_server, which is exempt,
 * so {@link #home} launches through an alarm. That alarm only fires at once with
 * {@code min_futurity=0}, which install.sh sets: in the global {@code alarm_manager_constants}
 * up to Android 11, in DeviceConfig {@code alarm_manager} from Android 12.
 *
 * Amazon's home is covered with a short-lived overlay until ours has drawn. The cover window is
 * added only for the handoff, so it does not exist over apps.
 *
 * Android 11 refuses an activity start from an app with no visible window, even one sent
 * through system_server by an alarm ("Background activity start ... callingUidHasAnyVisibleWindow:
 * false"). The cover is such a window, so the launch waits until the cover has reached the
 * window manager: {@link #launchWhenCovered} fires it from the cover's first draw pass.
 * Android 14 counts the cover only once the window manager has shown its surface, a frame or
 * more after that draw, so there the alarm is set {@link #SHOWN_DELAY_MS} later (issue #57).
 *
 * Waking from standby resumes whatever was on screen without a new window event. If the last
 * window was Amazon's home screen, ours is brought up at once. Amazon's other launcher screens,
 * such as its settings grid, are left alone, as is a wake before any window was seen.
 */
public final class Guard extends AccessibilityService {
    private static final String TAG = "dumbtv";
    static final String AMAZON = "com.amazon.tv.launcher";
    private static final String AMAZON_HOME = "com.amazon.tv.launcher.ui.HomeActivity_vNext";
    private static final long COVER_MAX_MS = 6000;
    private static final long REPEAT_MS = 1000;
    private static final long LAUNCH_FALLBACK_MS = 150;
    private static final long RETRY_MS = 250;
    private static final long RETRY_MAX_MS = 750;
    private static final long ARRIVAL_CHECK_MS = 1500;
    private static final long SHOWN_DELAY_MS = Build.VERSION.SDK_INT >= 34 ? 300 : 0;
    private static final int TASK_NONE = 0;
    private static final int TASK_HOME = 1;
    private static final int TASK_OTHER = 2;

    private static volatile boolean paused;
    private static Guard running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable uncover = this::uncover;
    private WindowManager windowManager;
    private WindowManager.LayoutParams coverParams;
    private View cover;
    private boolean coverAdded;
    private Runnable pendingLaunch;
    private Runnable arrivalCheck;
    private Runnable retry;
    private int handoff;
    private boolean acknowledged;
    private long lastLaunch;
    private long lastMove;
    private long retryUntil;
    private boolean onAmazonHome;
    private final BroadcastReceiver screenOn = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (!paused && onAmazonHome) launchWhenCovered(handoff);
        }
    };

    /** Lets Amazon's home stay on screen until the user leaves it. */
    static void pause() {
        paused = true;
        Keeper.pause();
    }

    static void home(Context c) {
        home(c, 0);
    }

    private static void home(Context c, long delayMs) {
        AlarmManager alarms = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        alarms.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + delayMs,
                launch(c));
    }

    /** Drops a queued launch, so it cannot pull the user back after they opened an app. */
    static void arrived(Context c) {
        AlarmManager alarms = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(launch(c));
    }

    /** Called when our Home activity has resumed; this is the handoff acknowledgement. */
    static void shown() {
        if (running != null) running.acknowledge();
    }

    private static PendingIntent launch(Context c) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setClass(c, Home.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT,
                ActivityOptions.makeCustomAnimation(c, 0, 0).toBundle());
    }

    static void amazon(Context c) {
        pause();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setPackage(AMAZON).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
        } catch (ActivityNotFoundException e) {
            paused = false;
        }
    }

    @Override
    protected void onServiceConnected() {
        running = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        coverParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE);
        coverParams.windowAnimations = 0;
        registerReceiver(screenOn, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(uncover);
        removePending();
        removeCover();
        try {
            unregisterReceiver(screenOn);
        } catch (IllegalArgumentException ignored) {
        }
        if (running == this) running = null;
        super.onDestroy();
    }

    private void removePending() {
        if (pendingLaunch != null) handler.removeCallbacks(pendingLaunch);
        if (arrivalCheck != null) handler.removeCallbacks(arrivalCheck);
        if (retry != null) handler.removeCallbacks(retry);
        pendingLaunch = null;
        arrivalCheck = null;
        retry = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = e.getPackageName(), cls = e.getClassName();
        if (pkg == null) return;
        if (!AMAZON.contentEquals(pkg)) {
            if ("com.android.systemui".contentEquals(pkg) || "android".contentEquals(pkg)) return;
            onAmazonHome = false;
            paused = false;
            handler.removeCallbacks(retry);
            // A genuine app event resets both debounces, so a quick app -> HOME press is
            // not swallowed as a duplicate Amazon event.
            lastLaunch = 0;
            lastMove = 0;
            acknowledged = false;
            if (!getPackageName().contentEquals(pkg)) {
                arrived(this);
                handoff++;
                removePending();
                uncover();
            }
            return;
        }
        onAmazonHome = cls != null && AMAZON_HOME.contentEquals(cls);
        if (paused || !onAmazonHome) return;
        long now = SystemClock.uptimeMillis();
        if (acknowledged && now - lastMove < 1500) return;
        if (now - lastLaunch < REPEAT_MS) return;
        lastLaunch = now;
        Options.dismissForHome();
        handoff++;
        acknowledged = false;
        removePending();
        cover();
        retryUntil = now + RETRY_MAX_MS;
        bringHome(handoff);
    }

    /** Fast path first; retry briefly, then use the cover/alarm fallback. */
    private void bringHome(final int token) {
        if (paused || token != handoff) return;
        int moved = moveFront();
        if (moved != TASK_NONE) {
            lastMove = SystemClock.uptimeMillis();
            if (moved == TASK_OTHER) startHomeDirect();
            scheduleArrivalCheck(token);
            return;
        }
        if (SystemClock.uptimeMillis() < retryUntil) {
            retry = () -> bringHome(token);
            handler.postDelayed(retry, RETRY_MS);
            return;
        }
        launchWhenCovered(token);
    }

    /**
     * Moving the existing Home task is not an activity start, so Android 11's background-start
     * rule does not apply. Only move when Home is already the task's top activity; if Options is
     * on top, the alarm path deliberately brings Home back. Empty recent tasks are skipped: moving
     * one cold-starts Home and leaves Amazon on screen.
     */
    private int moveFront() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager == null) return TASK_NONE;
        try {
            for (ActivityManager.AppTask task : manager.getAppTasks()) {
                ActivityManager.RecentTaskInfo info = task.getTaskInfo();
                ComponentName base = info == null || info.baseIntent == null
                        ? null : info.baseIntent.getComponent();
                if (base == null || !getPackageName().equals(base.getPackageName())
                        || !Home.class.getName().equals(base.getClassName())) continue;
                if (info.topActivity == null && info.baseActivity == null) continue;
                if (getPackageName().equals(info.topActivity.getPackageName())
                        && Home.class.getName().equals(info.topActivity.getClassName())) {
                    task.moveToFront();
                    return TASK_HOME;
                }
                // Options (or another activity) is above Home in this task. Move the task
                // forward, then start Home with CLEAR_TOP so the user never lands on Options.
                task.moveToFront();
                return TASK_OTHER;
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "could not move the Home task", e);
        }
        return TASK_NONE;
    }

    /** Direct start while the cover is visible; the alarm path remains the fallback. */
    private void startHomeDirect() {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setClass(this, Home.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(i);
        } catch (RuntimeException e) {
            Log.w(TAG, "direct Home start failed", e);
        }
    }

    /**
     * Shows the cover, then starts the launch once that window is visible to the window manager.
     * A cover that is already on screen gets no new draw pass, so the launch goes out at once;
     * otherwise it waits for the first pre-draw, which runs after the window's relayout. A short
     * fallback covers a window that never draws.
     */
    private void launchWhenCovered(final int token) {
        if (token != handoff) return;
        removePending();
        if (cover == null) {
            home(this);
            return;
        }
        boolean shown = coverAdded && cover.getVisibility() == View.VISIBLE;
        cover();
        if (shown) {
            home(this);
            return;
        }
        final ViewTreeObserver observer = cover.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                if (token != handoff) return true;
                if (pendingLaunch != null) handler.removeCallbacks(pendingLaunch);
                home(Guard.this, SHOWN_DELAY_MS);
                return true;
            }
        });
        pendingLaunch = () -> {
            if (token == handoff) home(Guard.this);
        };
        handler.postDelayed(pendingLaunch, LAUNCH_FALLBACK_MS);
    }

    /** moveToFront is not an arrival; wait for Home.shown() and fall back if it never comes. */
    private void scheduleArrivalCheck(final int token) {
        if (arrivalCheck != null) handler.removeCallbacks(arrivalCheck);
        arrivalCheck = () -> {
            if (token != handoff || acknowledged) return;
            launchWhenCovered(token);
        };
        handler.postDelayed(arrivalCheck, ARRIVAL_CHECK_MS);
    }

    private void acknowledge() {
        acknowledged = true;
        if (arrivalCheck != null) handler.removeCallbacks(arrivalCheck);
        uncover();
    }

    /** The cover window exists only during a handoff, never over an app. */
    private void cover() {
        if (windowManager == null || coverParams == null) return;
        if (!coverAdded) {
            cover = new View(this);
            cover.setBackgroundColor(Config.get(this).background());
            try {
                windowManager.addView(cover, coverParams);
                coverAdded = true;
            } catch (RuntimeException e) {
                cover = null;
                Log.w(TAG, "could not add the HOME cover", e);
                return;
            }
        }
        cover.setBackgroundColor(Config.get(this).background());
        cover.setVisibility(View.VISIBLE);
        handler.removeCallbacks(uncover);
        handler.postDelayed(uncover, COVER_MAX_MS);
    }

    private void uncover() {
        handler.removeCallbacks(uncover);
        removeCover();
    }

    private void removeCover() {
        if (coverAdded && cover != null && windowManager != null) {
            try {
                windowManager.removeView(cover);
            } catch (RuntimeException e) {
                Log.w(TAG, "could not remove the HOME cover", e);
            }
        }
        coverAdded = false;
        cover = null;
    }

    @Override
    public void onInterrupt() {}
}
