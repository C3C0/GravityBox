package com.ceco.gm2.gravitybox;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModElectronBeam {
    private static final String TAG = "ModElectronBeam";
    private static final String CLASS_DISPLAY_POWER_STATE = "com.android.server.power.DisplayPowerState";

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> clsDisplayPowerState = XposedHelpers.findClass(CLASS_DISPLAY_POWER_STATE, null);

            XposedHelpers.findAndHookMethod(clsDisplayPowerState, "prepareElectronBeam", int.class, 
                    new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    int ebMode = 
                            prefs.getBoolean(GravityBoxSettings.PREF_KEY_CRT_OFF_EFFECT, false) ? 1 : 2;
                    param.args[0] = ebMode;
                }
                
            });
            
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}