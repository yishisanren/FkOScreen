package io.github.fkoscreen.compat;

import android.opengl.Matrix;
import java.util.Set;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Native ColorOS 17 adapters for upstream 1.1 capabilities. */
public final class ColorOS17Features {
    private static void install(ClassLoader cl, String type, String method, XC_MethodHook callback) {
        Class<?> c = XposedHelpers.findClassIfExists(type, cl);
        if (c == null) {
            ColorOS17Entry.log("unavailable class: " + type);
            return;
        }
        Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(c, method, callback);
        ColorOS17Entry.log((hooks.isEmpty() ? "unavailable method: " : "installed ")
            + type + "." + method + " count=" + hooks.size());
    }

    public static void readMode(ClassLoader cl) {
        install(cl, "com.android.server.display.config.OplusDisplayFeatureConfig", "isSupportReadMode", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult(true); }
        });
        install(cl, "com.oplus.content.OplusFeatureConfigManager", "hasFeature", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if ("oplus.software.display.read_mode".equals(p.args[0])) p.setResult(true);
            }
        });
        // ColorOS 17 registers this feature through MatrixConverter; the old
        // color.eyeprotect.manager.OplusReadModeManager no longer exists.
        final float[] light = {0.4882f, 0.1382f, 0.1382f, 0f,
            0.4649f, 0.8149f, 0.4649f, 0f, 0.0469f, 0.0469f, 0.3969f, 0f, 0f, 0f, 0f, 1f};
        install(cl, "com.android.server.display.color.feature.OplusFeatureReadMode", "modifyMatrix", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!XposedHelpers.getBooleanField(p.thisObject, "mIsReadModeOpen")
                    || XposedHelpers.getIntField(p.thisObject, "mReadMode") != 1) return;
                float[] input = (float[]) p.args[0];
                if (input == null) p.setResult(light.clone());
                else {
                    float[] out = new float[16];
                    Matrix.multiplyMM(out, 0, light, 0, input, 0);
                    p.setResult(out);
                }
            }
        });
    }

    public static void thermal(ClassLoader cl) {
        String type = "com.android.server.display.feature.postprocess.OplusFeatureTemperatureLimitBrightness";
        install(cl, type, "isDragUpBlockedByTemperatureLimit", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("thermal_limit_bypass")) p.setResult(false);
            }
        });
        install(cl, type, "getTemperatureLimitBrightness", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("thermal_limit_bypass")) p.setResult(Float.NaN);
            }
        });
        install(cl, "com.android.server.display.feature.postprocess.edr.limiter.TemperatureLimiter", "setTemperature", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (RuntimeConfig.enabled("thermal_limit_bypass")) p.args[0] = 0;
            }
        });
    }
}
