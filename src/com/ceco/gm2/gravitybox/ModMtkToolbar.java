package com.ceco.gm2.gravitybox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModMtkToolbar {
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "GB:ModMtkToolbar";
    private static final boolean DEBUG = false;

    private static final String CLASS_MOBILE_STATE_TRACKER = 
            "com.android.systemui.statusbar.toolbar.QuickSettingsConnectionModel$MobileStateTracker";
    private static final String CLASS_WIFI_STATE_TRACKER = 
            "com.android.systemui.statusbar.toolbar.QuickSettingsConnectionModel$WifiStateTracker";

    private static List<String> mSlow2gStrings = new ArrayList<String>(Arrays.asList(
            "gemini_3g_disable_warning",
            "gemini_3g_disable_warning_cu",
            "gemini_3g_disable_warning_case1",
            "gemini_3g_disable_warning_case2"
    ));

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> mobileStateTrackerClass = 
                    XposedHelpers.findClass(CLASS_MOBILE_STATE_TRACKER, classLoader);
            final Class<?> wifiStateTrackerClass = 
                    XposedHelpers.findClass(CLASS_WIFI_STATE_TRACKER, classLoader);

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
                    if (mSlow2gStrings.contains(msgResName)) {
                        if (DEBUG) log("Skipping slow data warning dialog");
                        param.setResult(-1);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(wifiStateTrackerClass, "isClickable",
                    XC_MethodReplacement.returnConstant(true));

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
