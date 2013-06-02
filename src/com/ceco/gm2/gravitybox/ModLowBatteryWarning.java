package com.ceco.gm2.gravitybox;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class ModLowBatteryWarning {
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final boolean DEBUG = false;

    static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        try {
            XposedBridge.log("ModLowBatteryWarning: init");

            Class<?> classPowerUI = findClass("com.android.systemui.power.PowerUI", classLoader);

            // for debugging purposes - simulate low battery even if it's not
            if (DEBUG) {
                findAndHookMethod(classPowerUI, "findBatteryLevelBucket", int.class, new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(-1);
                    }

                });
            }

            findAndHookMethod(classPowerUI, "playLowBatterySound", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    final int batteryWarningPolicy = Integer.valueOf(
                            prefs.getString(GravityBoxSettings.PREF_KEY_LOW_BATTERY_WARNING_POLICY, "3"));
                    final boolean playSound = ((batteryWarningPolicy & GravityBoxSettings.BATTERY_WARNING_SOUND) != 0);

                    XposedBridge.log("ModLowBatteryWarning: playLowBatterySound called; playSound = " + playSound);
                    
                    if (!playSound)
                        param.setResult(null);
                }

            });

            findAndHookMethod(classPowerUI, "showLowBatteryWarning", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    final int batteryWarningPolicy = Integer.valueOf(
                            prefs.getString(GravityBoxSettings.PREF_KEY_LOW_BATTERY_WARNING_POLICY, "3"));
                    final boolean showPopup = ((batteryWarningPolicy & GravityBoxSettings.BATTERY_WARNING_POPUP) != 0);
                    
                    XposedBridge.log("ModLowBatteryWarning: showLowBatteryWarning called; showPopup = " + showPopup);

                    if (!showPopup)
                        param.setResult(null);
                }

            });

        } catch (Exception e) { XposedBridge.log(e); }
    }
}