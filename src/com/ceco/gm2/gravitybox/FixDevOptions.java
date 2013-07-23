package com.ceco.gm2.gravitybox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

import android.content.res.XModuleResources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

public class FixDevOptions {
    private static final String TAG = "FixDevOptions";
    public static final String PACKAGE_NAME = "com.android.settings";
    private static final String CLASS_PREF_GROUP = "android.preference.PreferenceGroup";
    private static final String CLASS_PREF_FRAGMENT = "android.preference.PreferenceFragment";
    private static final String CLASS_DEV_SETTINGS = "com.android.settings.DevelopmentSettings";
    

    private static PreferenceScreen mScreen;
    private static int mResId = 0;
    private static List<String> devOptKeys = new ArrayList<String>(Arrays.asList(
            "enforce_read_external",
            "local_backup_password",
            "debug_input_category",
            "debug_drawing_category",
            "debug_monitoring_category",
            "debug_applications_category"
    ));

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initZygote() {
        try {
            final Class<?> pgClass = XposedHelpers.findClass(CLASS_PREF_GROUP, null);
            final Class<?> pfClass = XposedHelpers.findClass(CLASS_PREF_FRAGMENT, null);

            log("hooking PreferenceFragment.addPreferencesFromResource method");
            XposedHelpers.findAndHookMethod(pfClass, "addPreferencesFromResource", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mResId == 0) return;

                    if (mResId == (Integer)param.args[0]) {
                        log("addPreferencesFromResource called from dev settings. Setting mScreen.");
                        mScreen = ((PreferenceFragment) param.thisObject).getPreferenceScreen();
                    }
                }
            });

            log("hooking PreferenceGroup.removePreference method");
            XposedHelpers.findAndHookMethod(pgClass, "removePreference", Preference.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    PreferenceGroup pg = (PreferenceGroup) param.thisObject;
                    if (pg != null && pg == mScreen) {
                        String prefKey = ((Preference)param.args[0]).getKey();
                        if (devOptKeys.contains(prefKey)) {
                            log("ignoring removePreference called from developer options; key=" + prefKey);
                            param.setResult(false);
                            return;
                        }
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    public static void initPackageResources(final XSharedPreferences prefs, final InitPackageResourcesParam resparam) {
        try {
            XModuleResources modRes = XModuleResources.createInstance(GravityBox.MODULE_PATH, resparam.res);
            resparam.res.setReplacement(PACKAGE_NAME, "array", "window_animation_scale_entries",
                    modRes.fwd(R.array.window_animation_scale_entries));
            resparam.res.setReplacement(PACKAGE_NAME, "array", "window_animation_scale_values",
                    modRes.fwd(R.array.window_animation_scale_values));
            resparam.res.setReplacement(PACKAGE_NAME, "array", "transition_animation_scale_entries",
                    modRes.fwd(R.array.transition_animation_scale_entries));
            resparam.res.setReplacement(PACKAGE_NAME, "array", "transition_animation_scale_values",
                    modRes.fwd(R.array.transition_animation_scale_values));
            resparam.res.setReplacement(PACKAGE_NAME, "array", "animator_duration_scale_entries",
                    modRes.fwd(R.array.animator_duration_scale_entries));
            resparam.res.setReplacement(PACKAGE_NAME, "array", "animator_duration_scale_values",
                    modRes.fwd(R.array.animator_duration_scale_values));
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classDevSettings = XposedHelpers.findClass(CLASS_DEV_SETTINGS, classLoader);

            log("hooking DeveloperSettings.onCreate method");
            XposedHelpers.findAndHookMethod(classDevSettings, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    PreferenceFragment pf = (PreferenceFragment) param.thisObject;
                    mResId = pf.getResources().getIdentifier("development_prefs", "xml", PACKAGE_NAME);
                    log("mResId=" + mResId);
                }
            });
        }
        catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}