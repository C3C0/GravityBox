package com.ceco.gm2.gravitybox;

import android.content.Context;
import android.content.res.Resources;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModMtkToolbar {
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "GB:ModMtkToolbar";
    private static final boolean DEBUG = false;

    private static final String CLASS_MOBILE_STATE_TRACKER = 
            "com.android.systemui.statusbar.toolbar.QuickSettingsConnectionModel$MobileStateTracker";

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> mobileStateTrackerClass = 
                    XposedHelpers.findClass(CLASS_MOBILE_STATE_TRACKER, classLoader);

            XposedHelpers.findAndHookMethod(mobileStateTrackerClass, "dataSwitchConfirmDlgMsg", 
                    long.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (!prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_MOBILE_DATA_SLOW2G_DISABLE, false)) return;

                    final int msgResId = (Integer) param.getResult();
                    if (DEBUG) log("dataSwitchConfirmDlgMsg return value = " + msgResId);
                    if (msgResId <= 0) return;

                    final Context context = (Context) XposedHelpers.getObjectField(
                            XposedHelpers.getSurroundingThis(param.thisObject), "mContext");
                    final Resources res = context.getResources();

                    final String msgResName = res.getResourceEntryName(msgResId);
                    if (DEBUG) log("Message resource name: " + msgResName);
                    if ("gemini_3g_disable_warning_case1".equals(msgResName)
                            || "gemini_3g_disable_warning_case2".equals(msgResName)) {
                        if (DEBUG) log("Skipping slow data warning dialog");
                        param.setResult(-1);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
