package com.ceco.gm2.gravitybox;

import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModSettings {
    private static final String TAG = "ModSettings";
    public static final String PACKAGE_NAME = "com.android.settings";
    private static final String CLASS_PU_SUMMARY = "com.android.settings.fuelgauge.PowerUsageSummary";

    private static void log (String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classPowerUsageSummary = XposedHelpers.findClass(CLASS_PU_SUMMARY, classLoader);

            XposedHelpers.findAndHookMethod(classPowerUsageSummary, "refreshStats", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    PreferenceGroup appListGroup = (PreferenceGroup) XposedHelpers.getObjectField(
                            param.thisObject, "mAppListGroup");

                    int prefCount = appListGroup.getPreferenceCount();
                    for (int i = 0; i < prefCount; i++) {
                        // We assume there's only one checkbox preference which is the battery percent checkbox
                        // Assumption is a mother of all fuckups, thus:
                        // TODO: make this more bullet-proof
                        Preference pref = appListGroup.getPreference(i);
                        if (pref instanceof CheckBoxPreference) {
                            log("CheckBoxPreference found: " + pref.getTitle() + " - removing");
                            appListGroup.removePreference(pref);
                            break;
                        }
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}