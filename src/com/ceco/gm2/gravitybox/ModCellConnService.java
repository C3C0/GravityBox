package com.ceco.gm2.gravitybox;

import android.app.Activity;
import android.content.Intent;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModCellConnService {
    public static final String PACKAGE_NAME = "com.mediatek.CellConnService";
    private static final String TAG = "ModCellConnService";
    private static final String CLASS_CONFIRM_DLG = "com.mediatek.CellConnService.ConfirmDlgActivity";
    private static final boolean DEBUG = false;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classConfirmDlg = XposedHelpers.findClass(CLASS_CONFIRM_DLG, classLoader);

            XposedHelpers.findAndHookMethod(classConfirmDlg, "onNewIntent", Intent.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    Intent intent = (Intent) param.args[0];
                    if (DEBUG) log("onNewIntent: " + ((intent == null) ? "NULL" : intent.toString()));

                    if (intent == null
                            || !prefs.getBoolean(GravityBoxSettings.PREF_KEY_ROAMING_WARNING_DISABLE, false)) {
                        return;
                    }

                    if (intent.getIntExtra("confirm_type", 0) == 0x195
                            && !intent.getBooleanExtra("confirm_roamingWithPrefer", false)) {
                        if (DEBUG) log("Roaming warning intent received - ignoring");
                        XposedHelpers.callMethod(param.thisObject, "sendConfirmResult", 0x195, true);
                        ((Activity)param.thisObject).finish();
                        param.setResult(null);
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}