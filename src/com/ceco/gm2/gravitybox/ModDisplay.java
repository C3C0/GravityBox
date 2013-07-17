package com.ceco.gm2.gravitybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XResources;
import android.os.Bundle;
import android.os.ResultReceiver;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModDisplay {
    private static final String TAG = "ModDisplay";
    private static final String CLASS_DISPLAY_POWER_CONTROLLER = "com.android.server.power.DisplayPowerController";

    public static final String ACTION_GET_AUTOBRIGHTNESS_CONFIG = "gravitybox.intent.action.GET_AUTOBRIGHTNESS_CONFIG";
    public static final String ACTION_SET_AUTOBRIGHTNESS_CONFIG = "gravitybox.intent.action.SET_AUTOBRIGHTNESS_CONFIG";
    public static final int RESULT_AUTOBRIGHTNESS_CONFIG = 0;

    private static Context mContext;
    private static Object mDisplayPowerController;
    private static int mScreenBrightnessRangeMinimum;
    private static int mScreenBrightnessRangeMaximum;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            log("Broadcast received: " + intent.toString());
            if(intent.getAction().equals(ACTION_GET_AUTOBRIGHTNESS_CONFIG) &&
                    intent.hasExtra("receiver")) {
                ResultReceiver receiver = intent.getParcelableExtra("receiver");
                Bundle data = new Bundle();
                Resources res = context.getResources();
                data.putIntArray("config_autoBrightnessLevels", 
                        res.getIntArray(res.getIdentifier(
                                "config_autoBrightnessLevels", "array", "android")));
                data.putIntArray("config_autoBrightnessLcdBacklightValues",
                        res.getIntArray(res.getIdentifier(
                                "config_autoBrightnessLcdBacklightValues", "array", "android")));
                receiver.send(RESULT_AUTOBRIGHTNESS_CONFIG, data);
            } else if (intent.getAction().equals(ACTION_SET_AUTOBRIGHTNESS_CONFIG)) {
                int[] luxArray = intent.getIntArrayExtra("config_autoBrightnessLevels");
                int[] brightnessArray = intent.getIntArrayExtra("config_autoBrightnessLcdBacklightValues");
                updateAutobrightnessConfig(luxArray, brightnessArray);
            }
        }
        
    };

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> classDisplayPowerController =
                    XposedHelpers.findClass(CLASS_DISPLAY_POWER_CONTROLLER, null);

            String brightnessMin = prefs.getString(GravityBoxSettings.PREF_KEY_BRIGHTNESS_MIN, "20");
            try {
                int bMin = Integer.valueOf(brightnessMin);
                XResources.setSystemWideReplacement(
                        "android", "integer", "config_screenBrightnessSettingMinimum", bMin);
                log("Minimum brightness value set to: " + bMin);
            } catch (NumberFormatException e) {
                XposedBridge.log(e);
            }

            XposedBridge.hookAllConstructors(classDisplayPowerController, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    log("DisplayPowerController constructed");
                    if (param.args.length < 2) {
                        log("Unsupported parameters. Aborting.");
                        return;
                    }
                    mContext = (Context) param.args[1];
                    if (mContext == null) {
                        log("Context is null. Aborting.");
                        return;
                    }

                    mDisplayPowerController = param.thisObject;
                    mScreenBrightnessRangeMinimum = XposedHelpers.getIntField(
                            param.thisObject, "mScreenBrightnessRangeMinimum");
                    mScreenBrightnessRangeMaximum = XposedHelpers.getIntField(
                            param.thisObject, "mScreenBrightnessRangeMaximum");

                    prefs.reload();
                    String config = prefs.getString(GravityBoxSettings.PREF_KEY_AUTOBRIGHTNESS, null);
                    if (config != null) {
                        String[] luxValues = config.split("\\|")[0].split(",");
                        String[] brightnessValues = config.split("\\|")[1].split(",");
                        int[] luxArray = new int[luxValues.length];
                        int index = 0;
                        for(String s : luxValues) {
                            luxArray[index++] = Integer.valueOf(s);
                        }
                        int[] brightnessArray = new int[brightnessValues.length];
                        index = 0;
                        for(String s : brightnessValues) {
                            brightnessArray[index++] = Integer.valueOf(s);
                        }
                        updateAutobrightnessConfig(luxArray, brightnessArray);
                    }

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(ACTION_GET_AUTOBRIGHTNESS_CONFIG);
                    intentFilter.addAction(ACTION_SET_AUTOBRIGHTNESS_CONFIG);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(classDisplayPowerController, 
                    "clampScreenBrightness", int.class, new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            return XposedHelpers.callMethod(param.thisObject, "clamp", param.args[0],
                                    mScreenBrightnessRangeMinimum, mScreenBrightnessRangeMaximum);
                        }
            });

        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void updateAutobrightnessConfig(int[] lux, int[] brightness) {
        if (mDisplayPowerController == null || mContext == null) return;

        Resources res = mContext.getResources();

        int screenBrightnessDim = res.getInteger(res.getIdentifier(
                "config_screenBrightnessDim", "integer", "android"));
        int screenBrightnessMinimum = res.getInteger(res.getIdentifier(
                "config_screenBrightnessSettingMinimum", "integer", "android"));
        screenBrightnessMinimum = Math.min(screenBrightnessDim, screenBrightnessMinimum);

        boolean useSwAutobrightness = XposedHelpers.getBooleanField(
                mDisplayPowerController, "mUseSoftwareAutoBrightnessConfig");

        if (useSwAutobrightness) {
            Object autoBrightnessSpline = XposedHelpers.callMethod(
                    mDisplayPowerController, "createAutoBrightnessSpline", lux, brightness);
            XposedHelpers.setObjectField(mDisplayPowerController, 
                    "mScreenAutoBrightnessSpline", autoBrightnessSpline);
            if (autoBrightnessSpline != null) {
                if (brightness[0] < screenBrightnessMinimum) {
                    screenBrightnessMinimum = brightness[0];
                }
            }
        }

        mScreenBrightnessRangeMinimum = (Integer) XposedHelpers.callMethod(
                mDisplayPowerController, "clampAbsoluteBrightness", screenBrightnessMinimum);

        log("Autobrightness config updated");
    }
}