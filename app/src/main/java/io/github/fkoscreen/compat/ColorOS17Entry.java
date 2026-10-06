package io.github.fkoscreen.compat;

import android.content.Context;
import android.os.Binder;
import android.util.Pair;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Verified against the classes shipped in PME110_17.0.0.102(CN01), API 37. */
public final class ColorOS17Entry implements IXposedHookLoadPackage {
    private static final Set<String> logged = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static volatile float panelBrightness, panelNit, hbmBrightness, hbmNit;
    private static volatile Object model;
    private static volatile int appUid = -1;
    private static volatile Object colorController;
    private static volatile Object displayService;
    private static final Set<Object> powerControllers = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());
    private static final Set<Object> uiExtensions = Collections.synchronizedSet(
        Collections.newSetFromMap(new java.util.WeakHashMap<Object, Boolean>()));
    private static volatile float uiMaximum;
    private static volatile long uiMaximumAt;
    private static final java.util.Map<Object, java.lang.ref.WeakReference<Object>> sliderExtensions =
        Collections.synchronizedMap(new java.util.WeakHashMap<Object, java.lang.ref.WeakReference<Object>>());

    public static void log(String text) { XposedBridge.log("FkOScreen17: " + text); }
    private static void once(String text) { if (logged.add(text)) log(text); }

    private static Class<?> find(ClassLoader cl, String... names) {
        for (String name : names) {
            Class<?> c = XposedHelpers.findClassIfExists(name, cl);
            if (c != null) return c;
        }
        log("unavailable class: " + names[0]);
        return null;
    }

    private static void hook(Class<?> c, String name, XC_MethodHook callback) {
        if (c == null) return;
        Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(c, name, callback);
        if (hooks.isEmpty()) log("unavailable method: " + c.getName() + "." + name);
        else log("installed " + c.getName() + "." + name + " count=" + hooks.size());
    }

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        log("loaded " + p.packageName + " process=" + p.processName + " build=1.0.0-coloros17.1");
        switch (p.packageName) {
            case "android":
                brightness(p.classLoader);
                foss(p.classLoader);
                hdr(p.classLoader);
                hdrServer(p.classLoader);
                color(p.classLoader);
                break;
            case "com.android.systemui":
                hdr(p.classLoader);
                brightnessUi(p.classLoader);
                break;
            case "com.android.settings": settingsUi(p.classLoader); break;
            case "com.android.providers.settings": provider(p.classLoader); break;
        }
    }

    private static float number(Object value) { return ((Number) value).floatValue(); }
    private static float original(Object receiver, String method, Object... args) throws Throwable {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < types.length; i++) types[i] = float.class;
        for (Class<?> c = receiver.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(method, types);
                m.setAccessible(true);
                return number(XposedBridge.invokeOriginalMethod(m, receiver, args));
            } catch (NoSuchMethodException ignored) { }
        }
        throw new NoSuchMethodException(receiver.getClass().getName() + "." + method);
    }
    private static void captureModel(Object m) {
        if (model == m && panelBrightness > 0) return;
        try {
            float peakB = original(m, "getMaxPanelBrightness");
            float peakN = original(m, "getMaxPanelNit");
            // Preserve the original HBM feature's requested 1250 nits; convert with this panel's curve.
            float requested = Math.min(1250f, peakN);
            float hbmB = original(m, "getBrightnessFromNit", requested);
            if (!Float.isFinite(peakB) || !Float.isFinite(peakN) || !Float.isFinite(hbmB)
                || peakB <= 0 || peakN <= 0 || hbmB <= 0) return;
            model = m;
            panelBrightness = peakB; panelNit = peakN;
            hbmBrightness = Math.min(hbmB, peakB); hbmNit = requested;
            log("native panel: peak=" + peakB + " / " + peakN + "nits; HBM=" + hbmBrightness + " / " + hbmNit + "nits");
        } catch (Throwable t) { log("panel capture failed: " + t); }
    }
    private static Float targetBrightness() {
        if (!RuntimeConfig.manualMode() || panelBrightness <= 0) return null;
        if (RuntimeConfig.enabled("manual_peak_brightness")) return panelBrightness;
        if (RuntimeConfig.enabled("manual_hbm_brightness")) return hbmBrightness;
        return null;
    }
    private static Float targetNit() {
        if (targetBrightness() == null) return null;
        return RuntimeConfig.enabled("manual_peak_brightness") ? panelNit : hbmNit;
    }
    private static void brightness(ClassLoader cl) {
        Class<?> mc = find(cl, "com.android.server.display.model.OplusDisplayBrightnessModel");
        if (mc != null) {
            XposedBridge.hookAllConstructors(mc, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) { captureModel(p.thisObject); }
            });
            hook(mc, "getMaxBrightness", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (panelBrightness <= 0) captureModel(p.thisObject);
                    Float target = targetBrightness();
                    if (target != null) { p.setResult(target); once("applied model maximum=" + target); }
                }
            });
        }
        XC_MethodHook limit = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Float target = targetBrightness();
                if (target != null) { p.setResult(target); once("applied manual maximum=" + target + " via " + p.method.getName()); }
            }
        };
        hook(find(cl, "com.android.server.display.feature.postprocess.OplusFeatureIncreaseBrightnessRange"), "getScreenNormalMaxBrightness", limit);
        hook(find(cl, "com.android.server.display.feature.postprocess.OplusBrightnessPostProcessingManager"), "getScreenNormalMaxBrightness", limit);
        Class<?> dpc = find(cl, "com.android.server.display.DisplayPowerController");
        if (dpc != null) XposedBridge.hookAllConstructors(dpc, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { powerControllers.add(p.thisObject); }
        });
        hook(dpc, "getScreenBrightnessNormalMaximum", limit);
        hook(dpc, "getBrightnessInfo", brightnessInfoHook());
        hook(find(cl, "com.android.server.display.DisplayManagerService"), "getBrightnessInfoInternal", brightnessInfoHook());
        Class<?> window = find(cl, "com.android.server.display.feature.postprocess.OplusFeatureWindowBrightness");
        XC_MethodHook windowLimit = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                Float target = targetNit();
                if (target == null) return;
                float old = XposedHelpers.getFloatField(p.thisObject, "mLimitNit");
                p.setObjectExtra("fkoOldLimit", old);
                XposedHelpers.setFloatField(p.thisObject, "mLimitNit", target);
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Object old = p.getObjectExtra("fkoOldLimit");
                if (old != null) XposedHelpers.setFloatField(p.thisObject, "mLimitNit", (Float) old);
            }
        };
        hook(window, "postProcessing", windowLimit);
        Class<?> thermal = find(cl, "com.android.server.display.feature.postprocess.OplusFeatureTemperatureLimitBrightness");
        hook(thermal, "postProcessing", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("thermal_limit_bypass")) p.setResult(null);
            }
        });
        hook(thermal, "getbrightness", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("thermal_limit_bypass")) p.setResult(p.args[0]);
            }
        });
        hook(thermal, "isMaxBrightnessLimit", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("thermal_limit_bypass")) p.setResult(false);
            }
        });
        // Expose state in the existing dumpsys display output for device verification.
        hook(mc, "dump", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                java.io.PrintWriter out = (java.io.PrintWriter) p.args[0];
                out.println("FkOScreen17: panel=" + panelBrightness + " panelNit=" + panelNit
                    + " hbm=" + hbmBrightness + " manual=" + RuntimeConfig.manualMode()
                    + " target=" + targetBrightness() + " observers=" + RuntimeConfig.isObserving());
            }
        });
    }

    private static XC_MethodHook brightnessInfoHook() {
        return new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                Float target = targetBrightness();
                Object info = p.getResult();
                if (target == null || info == null) return;
                // Do not mutate the shared native BrightnessInfo: disabling must recover its native maximum.
                Class<?> c = info.getClass();
                Constructor<?> ctor = c.getConstructor(float.class, float.class, float.class, float.class,
                    int.class, float.class, int.class, boolean.class);
                Object copy = ctor.newInstance(XposedHelpers.getFloatField(info, "brightness"),
                    XposedHelpers.getFloatField(info, "adjustedBrightness"),
                    XposedHelpers.getFloatField(info, "brightnessMinimum"), target,
                    XposedHelpers.getIntField(info, "highBrightnessMode"),
                    XposedHelpers.getFloatField(info, "highBrightnessTransitionPoint"),
                    XposedHelpers.getIntField(info, "brightnessMaxReason"),
                    XposedHelpers.getBooleanField(info, "isBrightnessOverrideByWindow"));
                p.setResult(copy);
            }
        };
    }

    private static void brightnessUi(ClassLoader cl) {
        Class<?> controller = find(cl, "com.android.systemui.qs.deprecated.brightness.BrightnessController",
            "com.android.systemui.settings.brightness.BrightnessController");
        hook(controller, "getBrightnessInfo", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Object info = p.getResult();
                // The server has already converted the panel's limit; do not use system_server static caches in SystemUI.
                if (info != null) {
                    float max = XposedHelpers.getFloatField(info, "brightnessMaximum");
                    XposedHelpers.setFloatField(p.thisObject, "mBrightnessMax", max);
                    once("SystemUI slider maximum=" + max);
                }
            }
        });
        Class<?> extension = find(cl, "com.oplus.systemui.qs.impl.OplusBrightnessControllerExImpl");
        Class<?> flavor = find(cl, "com.oplus.systemui.qs.impl.FlavorOneBrightnessControllerExImpl");
        XC_MethodHook max = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Float target = uiManualMaximum(p.thisObject);
                if (target != null) { p.setResult((int) target.floatValue()); once("OPlus slider maximum=" + target); }
            }
        };
        hook(extension, "getBrightnessMax", max);
        hook(flavor, "getBrightnessSecMax", max);
        hook(flavor, "getMaxBrightness", max);
        XC_MethodHook bindControl = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                uiExtensions.add(p.thisObject);
                XposedHelpers.setAdditionalInstanceField(p.thisObject, "fkoSlider", p.args[0]);
                sliderExtensions.put(p.args[0], new java.lang.ref.WeakReference<Object>(p.thisObject));
                once("OPlus control bound " + p.thisObject.getClass().getName() + " -> " + p.args[0].getClass().getName());
            }
        };
        hook(flavor, "initControl", bindControl);
        hook(find(cl, "com.android.systemui.qs.OplusBrightnessControllerEx"), "initControl", bindControl);
        XC_MethodHook range = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                java.lang.ref.WeakReference<Object> owner = sliderExtensions.get(p.thisObject);
                Object extension = owner == null ? null : owner.get();
                if (extension == null || !(Boolean) XposedHelpers.callMethod(extension, "isMultiBits")) return;
                Float target = uiManualMaximum(extension);
                if (target == null) return;
                int min = (Integer) XposedHelpers.callMethod(extension, "getBrightnessMin");
                p.args[0] = (int) target.floatValue() - min;
                once("OPlus slider range applied=" + p.args[0] + " class=" + p.thisObject.getClass().getName());
            }
        };
        hook(find(cl, "com.oplus.systemui.qs.slider.OplusQsBrightnessSliderController"), "setMax", range);
        hook(find(cl, "com.oplus.systemui.qs.OplusBrightnessSliderController"), "setMax", range);
        hook(find(cl, "com.android.systemui.qs.brightness.BrightnessSliderController"), "setMax", range);
    }

    private static Float uiManualMaximum(Object extension) {
        if (!RuntimeConfig.manualMode() || (!RuntimeConfig.enabled("manual_hbm_brightness")
            && !RuntimeConfig.enabled("manual_peak_brightness"))) return null;
        long now = android.os.SystemClock.uptimeMillis();
        if (uiMaximum > 0 && now - uiMaximumAt < 100) return uiMaximum;
        Context c = (Context) XposedHelpers.getObjectField(extension, "mContext");
        android.hardware.display.DisplayManager dm = c.getSystemService(android.hardware.display.DisplayManager.class);
        android.view.Display display = dm.getDisplay(0);
        if (display == null) return null;
        Object info = XposedHelpers.callMethod(display, "getBrightnessInfo");
        if (info == null) return null;
        uiMaximum = XposedHelpers.getFloatField(info, "brightnessMaximum");
        uiMaximumAt = now;
        return uiMaximum > 0 ? uiMaximum : null;
    }

    private static void refreshUiRanges() {
        uiMaximumAt = 0;
        if (uiExtensions.isEmpty()) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                Object[] extensions;
                synchronized (uiExtensions) { extensions = uiExtensions.toArray(); }
                for (Object extension : extensions) {
                    try {
                        if (!(Boolean) XposedHelpers.callMethod(extension, "isMultiBits")) continue;
                        Object slider = XposedHelpers.getAdditionalInstanceField(extension, "fkoSlider");
                        int max = (Integer) XposedHelpers.callMethod(extension, "getMaxBrightness");
                        int min = (Integer) XposedHelpers.callMethod(extension, "getBrightnessMin");
                        if (slider != null && max > min) XposedHelpers.callMethod(slider, "setMax", max - min);
                    } catch (Throwable t) { once("UI brightness refresh failed: " + t); }
                }
            }
        }, 150);
    }

    private static void foss(ClassLoader cl) {
        XC_MethodHook disabled = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("foss_bypass")) { p.setResult(false); once("FOSS reduction bypass applied"); }
            }
        };
        hook(find(cl, "com.android.server.display.feature.postprocess.OplusFeatureReduceBrightness"), "getReduceInfo", disabled);
        hook(find(cl, "com.android.server.display.feature.panel.OplusFeatureFoss"), "getFossInfo", disabled);
    }

    private static void hdr(ClassLoader cl) {
        hook(find(cl, "android.view.Display"), "getHdrSdrRatio", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("hdr_ratio_fix")) { p.setResult(1.5625f); once("HDR ratio applied=1.5625"); }
            }
        });
    }

    private static void hdrServer(ClassLoader cl) {
        Class<?> service = find(cl, "com.android.server.display.DisplayManagerService");
        if (service != null) XposedBridge.hookAllConstructors(service, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { displayService = p.thisObject; }
        });
        hook(service, "getDisplayInfoInternal", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!RuntimeConfig.enabled("hdr_ratio_fix") || p.getResult() == null) return;
                Object info = p.getResult();
                // Return a copy: writing into the native cached DisplayInfo leaves HDR fixed after disabling.
                Object copy = XposedHelpers.newInstance(info.getClass(), info);
                XposedHelpers.setFloatField(copy, "hdrSdrRatio", 1.5625f);
                p.setResult(copy);
                once("HDR DisplayInfo copy applied=1.5625");
            }
        });
    }

    private static void settingsUi(ClassLoader cl) {
        hook(find(cl, "com.oplus.settings.utils.FeatureUtils"), "hasAppFeature", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("dark_mode_styles") && p.args.length > 0
                    && "com.android.settings_dark_mode_style_enable".equals(p.args[0])) {
                    p.setResult(true); once("dark mode styles enabled");
                }
            }
        });
        XC_MethodHook enable = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("color_ball_anchor")) p.args[0] = true;
            }
        };
        hook(find(cl, "com.oplus.settings.feature.display.screencolortemp.ScreenColorTemperateBallPreference"), "setEnabled", enable);
        hook(find(cl, "com.oplus.settings.feature.display.screencolortemp.ScreenColorTemperateBall"), "setEnabled", enable);
        hook(find(cl, "com.oplus.settings.feature.display.protecteyes.ColorModeFragment"), "updateScreenColorTemperatureState", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!RuntimeConfig.enabled("color_ball_anchor")) return;
                Object pref = XposedHelpers.getObjectField(p.thisObject, "mScreenColorTempBallPref");
                if (pref != null) XposedHelpers.callMethod(pref, "setEnabled", true);
                Object summary = XposedHelpers.getObjectField(p.thisObject, "mScreenColorSummary");
                if (summary != null) XposedHelpers.callMethod(summary, "setVisible", false);
            }
        });
    }

    private static void color(ClassLoader cl) {
        final Class<?> rgb = find(cl, "com.android.server.display.color.model.RGB");
        hook(find(cl, "com.android.server.display.color.config.OplusColorConfig"), "interpolate", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                if (!RuntimeConfig.enabled("color_ball_anchor") || rgb == null) return;
                int[] gains = RuntimeConfig.ballRgb();
                if (gains[0] == 1000 && gains[1] == 1000 && gains[2] == 1000) return;
                Object value = p.getResult();
                if (!(value instanceof Pair)) return;
                Pair<?, ?> pair = (Pair<?, ?>) value;
                Object first = anchoredRgb(rgb, pair.first, gains);
                Object second = anchoredRgb(rgb, pair.second, gains);
                p.setResult(new Pair<Object, Object>(first, second));
                once("CCT anchor applied RGB=" + gains[0] + "," + gains[1] + "," + gains[2]);
            }
        });
        Class<?> controller = find(cl, "com.android.server.display.color.control.DisplayColorController");
        hook(controller, "onSetUp", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { colorController = p.thisObject; }
        });
        // Native updateEyeProtectRGB already schedules a new CCT calculation on ColorOS 17.
        hook(controller, "updateEyeProtectRGB", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                RuntimeConfig.refreshBall();
                redrawColor(p.thisObject);
            }
        });
    }

    private static void redrawColor(Object controller) {
        if (controller == null) return;
        Object colorContext = XposedHelpers.callMethod(controller, "getColorContext");
        boolean eye = (Boolean) XposedHelpers.callMethod(colorContext, "isEyeProtectEnabled");
        boolean ambient = (Boolean) XposedHelpers.callMethod(colorContext, "isColorTemperatureRegulationEnabled");
        boolean rhythm = (Boolean) XposedHelpers.callMethod(colorContext, "isComfortableRhythmEnabled");
        if (!eye && !ambient && !rhythm) return;
        Object coordinator = XposedHelpers.callMethod(controller, "getAnimationCoordinator");
        Object animator = XposedHelpers.getObjectField(coordinator, "mRampAnimator");
        int current = (Integer) XposedHelpers.callMethod(coordinator, "getCurrentCCT");
        if (current <= 0) return;
        // Also redraw on disable, to remove the previous anchor without requiring a changed CCT.
        XposedHelpers.callMethod(animator, "showCCTToScreen", current, true);
        once("ColorOS17 realtime CCT redraw applied");
    }

    public static void configurationChanged(String key) {
        Object service = displayService;
        if ("hdr_ratio_fix".equals(key) && service != null) {
            try {
                Object lock = XposedHelpers.getObjectField(service, "mSyncRoot");
                synchronized (lock) {
                    Object mapper = XposedHelpers.getObjectField(service, "mLogicalDisplayMapper");
                    Object display = XposedHelpers.callMethod(mapper, "getDisplayLocked", 0);
                    // A basic-change event does not invalidate the HDR field in Android 17 clients.
                    Class<?> events = Class.forName("android.hardware.display.DisplayManagerGlobal");
                    XposedHelpers.callStaticMethod(events, "invalidateLocalDisplayInfoCaches");
                    int mask = XposedHelpers.getStaticIntField(events, "EVENT_DISPLAY_BASIC_CHANGED")
                        | XposedHelpers.getStaticIntField(events, "EVENT_DISPLAY_HDR_SDR_RATIO_CHANGED");
                    if (display != null) XposedHelpers.callMethod(service, "sendDisplayEventsLocked", display, mask);
                }
            } catch (Throwable t) { once("HDR refresh failed: " + t); }
        }
        if (key.startsWith("manual_") || "thermal_limit_bypass".equals(key) || "foss_bypass".equals(key)
            || "screen_brightness_mode".equals(key)) {
            refreshUiRanges();
            for (Object controller : powerControllers) {
                try { XposedHelpers.callMethod(controller, "sendUpdatePowerState"); }
                catch (Throwable t) { once("brightness refresh failed: " + t); }
            }
        }
        final Object controller = colorController;
        if ("color_ball_anchor".equals(key) && controller != null) {
            android.os.Looper looper = (android.os.Looper) XposedHelpers.getObjectField(controller, "mColorLooper");
            new android.os.Handler(looper).post(new Runnable() {
                @Override public void run() {
                    try { redrawColor(controller); }
                    catch (Throwable t) { once("color refresh failed: " + t); }
                }
            });
        }
    }

    private static Object anchoredRgb(Class<?> rgb, Object nativeRgb, int[] gains) {
        if (nativeRgb == null) return null;
        float r = Math.round(XposedHelpers.getIntField(nativeRgb, "red") * gains[0] / 1000f);
        float g = Math.round(XposedHelpers.getIntField(nativeRgb, "green") * gains[1] / 1000f);
        float b = Math.round(XposedHelpers.getIntField(nativeRgb, "blue") * gains[2] / 1000f);
        Object coeff = XposedHelpers.getObjectField(nativeRgb, "protectCoeff");
        return XposedHelpers.newInstance(rgb, r, g, b, coeff);
    }

    private static boolean moduleCaller() {
        Context c = RuntimeConfig.context();
        if (c == null) return false;
        try {
            if (appUid < 0) appUid = c.getPackageManager().getApplicationInfo(RuntimeConfig.PACKAGE, 0).uid;
            return Binder.getCallingUid() == appUid;
        } catch (Throwable ignored) { return false; }
    }
    private static boolean allowedKey(Object key) {
        if (!(key instanceof String)) return false;
        for (String s : RuntimeConfig.KEYS) if (s.equals(key)) return true;
        return "eyeprotect_rgb".equals(key) || "color_ball_last_pointx".equals(key)
            || "color_ball_last_pointy".equals(key) || "color_temperate_ball_mode".equals(key);
    }
    private static void provider(ClassLoader cl) {
        Class<?> c = find(cl, "com.android.providers.settings.SettingsProvider");
        hook(c, "enforceRestrictedSystemSettingsMutationForCallingPackage", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (allowedKey(p.args[1]) && moduleCaller()) p.setResult(null);
            }
        });
        hook(c, "warnOrThrowForUndesiredSecureSettingsMutationForTargetSdk", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (allowedKey(p.args[2]) && moduleCaller()) p.setResult(null);
            }
        });
    }
}
