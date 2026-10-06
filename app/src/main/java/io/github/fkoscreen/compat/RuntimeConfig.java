package io.github.fkoscreen.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Collections;
import android.app.AndroidAppHelper;
import de.robv.android.xposed.XposedHelpers;

/** Cross-process settings, observed once instead of opening preference files per frame. */
public final class RuntimeConfig {
    public static final String PACKAGE = "io.github.benbaobaoshigemi.fkoscreen";
    public static final String[] KEYS = {"manual_hbm_brightness", "manual_peak_brightness",
        "thermal_limit_bypass", "foss_bypass", "hdr_ratio_fix", "dark_mode_styles", "color_ball_anchor"};
    private static final boolean[] DEFAULTS = {true, false, false, true, true, true, true};
    private static final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
    private static volatile Context context;
    private static volatile boolean observing;
    private static volatile long lastObserverAttempt;
    private static final Set<String> observed = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public static Context context() {
        Context c = context;
        if (c != null) {
            if (!observing && android.os.SystemClock.uptimeMillis() - lastObserverAttempt > 1000) attach(c);
            return c;
        }
        try {
            c = AndroidAppHelper.currentApplication();
            if (c == null) {
                Class<?> at = Class.forName("android.app.ActivityThread");
                Object thread = XposedHelpers.callStaticMethod(at, "currentActivityThread");
                if (thread != null) c = (Context) XposedHelpers.callMethod(thread, "getSystemContext");
            }
            if (c != null) attach(c);
        } catch (Throwable ignored) { }
        return context;
    }

    public static synchronized void attach(Context c) {
        context = c;
        if (observing || Looper.getMainLooper() == null) return;
        lastObserverAttempt = android.os.SystemClock.uptimeMillis();
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            if (XposedHelpers.callStaticMethod(sm, "getService", "content") == null) return;
            for (String key : KEYS) observe(c, key);
            observe(c, "eyeprotect_rgb");
            observe(c, "screen_brightness_mode");
            observing = true;
        } catch (Throwable t) {
            ColorOS17Entry.log("config observer unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void observe(final Context c, final String key) {
        refresh(c, key);
        if (observed.contains(key)) return;
        c.getContentResolver().registerContentObserver(Settings.System.getUriFor(key), false,
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override public void onChange(boolean selfChange) {
                    refresh(c, key);
                    ColorOS17Entry.configurationChanged(key);
                    if (!"eyeprotect_rgb".equals(key))
                        ColorOS17Entry.log("config " + key + "=" + values.get(key));
                }
            });
        observed.add(key);
    }

    private static void refresh(Context c, String key) {
        try {
            String value = Settings.System.getString(c.getContentResolver(), key);
            if (value == null) values.remove(key); else values.put(key, value);
        } catch (Throwable ignored) { }
    }

    public static boolean enabled(String key) {
        context();
        String value = values.get(key);
        if (value != null) return "1".equals(value);
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return DEFAULTS[i];
        return false;
    }

    public static boolean isObserving() { return observing; }

    public static boolean manualMode() {
        Context c = context();
        if (!values.containsKey("screen_brightness_mode") && c != null) refresh(c, "screen_brightness_mode");
        return "0".equals(values.get("screen_brightness_mode"));
    }

    public static int[] ballRgb() {
        context();
        String rgb = values.get("eyeprotect_rgb");
        if (rgb == null) return new int[]{1000, 1000, 1000};
        try {
            String[] parts = rgb.split(",");
            if (parts.length < 3) return new int[]{1000, 1000, 1000};
            int[] out = new int[3];
            for (int i = 0; i < 3; i++) out[i] = Math.max(0, Math.min(1000, Integer.parseInt(parts[i].trim())));
            return out;
        } catch (RuntimeException ignored) { return new int[]{1000, 1000, 1000}; }
    }

    public static void refreshBall() {
        Context c = context();
        if (c != null) refresh(c, "eyeprotect_rgb");
    }

    /** Called by the preserved UI at launch and after every switch change. */
    public static void syncAppPrefs(Context c) {
        SharedPreferences p = c.getSharedPreferences("fkoscreen_prefs", Context.MODE_PRIVATE);
        try {
            for (int i = 0; i < KEYS.length; i++)
                Settings.System.putInt(c.getContentResolver(), KEYS[i], p.getBoolean(KEYS[i], DEFAULTS[i]) ? 1 : 0);
        } catch (Throwable t) {
            android.util.Log.w("FkOScreen17", "Settings permission not granted; allow modify system settings", t);
        }
    }
}
