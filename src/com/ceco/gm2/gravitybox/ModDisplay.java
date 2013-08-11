package com.ceco.gm2.gravitybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XResources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModDisplay {
    private static final String TAG = "ModDisplay";
    private static final String CLASS_DISPLAY_POWER_CONTROLLER = "com.android.server.power.DisplayPowerController";
    private static final String CLASS_LIGHT_SERVICE_LIGHT = "com.android.server.LightsService$Light";
    private static final boolean DEBUG = false;

    public static final String ACTION_GET_AUTOBRIGHTNESS_CONFIG = "gravitybox.intent.action.GET_AUTOBRIGHTNESS_CONFIG";
    public static final String ACTION_SET_AUTOBRIGHTNESS_CONFIG = "gravitybox.intent.action.SET_AUTOBRIGHTNESS_CONFIG";
    public static final int RESULT_AUTOBRIGHTNESS_CONFIG = 0;

    private static final int LIGHT_ID_BUTTONS = 2;
    private static final int LIGHT_ID_NOTIFICATIONS = 4;
    private static final int LIGHT_ID_ATTENTION = 5;

    private static Context mContext;
    private static Object mDisplayPowerController;
    private static int mScreenBrightnessRangeMinimum;
    private static int mScreenBrightnessRangeMaximum;
    private static String mButtonBacklightMode;
    private static boolean mButtonBacklightNotif;
    private static PowerManager mPm;
    private static boolean mPendingNotif = false;

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
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_BUTTON_BACKLIGHT_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BB_MODE)) {
                    mButtonBacklightMode = intent.getStringExtra(GravityBoxSettings.EXTRA_BB_MODE);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BB_NOTIF)) {
                    mButtonBacklightNotif = intent.getBooleanExtra(GravityBoxSettings.EXTRA_BB_NOTIF, false);
                    if (!mButtonBacklightNotif) {
                        mPendingNotif = false;
                    }
                }
            }
        }
        
    };

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> classDisplayPowerController =
                    XposedHelpers.findClass(CLASS_DISPLAY_POWER_CONTROLLER, null);
            final Class<?> classLight = XposedHelpers.findClass(CLASS_LIGHT_SERVICE_LIGHT, null);

            String brightnessMin = prefs.getString(GravityBoxSettings.PREF_KEY_BRIGHTNESS_MIN, "20");
            mButtonBacklightMode = prefs.getString(
                    GravityBoxSettings.PREF_KEY_BUTTON_BACKLIGHT_MODE, GravityBoxSettings.BB_MODE_DEFAULT);
            mButtonBacklightNotif = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS, false);

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
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BUTTON_BACKLIGHT_CHANGED);
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

            XposedHelpers.findAndHookMethod(classLight, "setLightLocked",
                    int.class, int.class, int.class, int.class, int.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    int id = XposedHelpers.getIntField(param.thisObject, "mId");
                    if (DEBUG ) log("lightId=" + id + "; color=" + param.args[0] + 
                            "; mode=" + param.args[1] + "; " + "onMS=" + param.args[2] + 
                            "; offMS=" + param.args[3] + "; bMode=" + param.args[4]);

                    if (mPm == null) {
                        Object lightService = XposedHelpers.getSurroundingThis(param.thisObject);
                        Context context = (Context) XposedHelpers.getObjectField(lightService, "mContext");
                        mPm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    }

                    if (id == LIGHT_ID_BUTTONS) {
                        if (mButtonBacklightMode.equals(GravityBoxSettings.BB_MODE_DISABLE) && !mPendingNotif) {
                            param.args[0] = param.args[1] = param.args[2] = param.args[3] = param.args[4] = 0;
                            if (DEBUG) log("Button backlight disabled. Turning off");
                            return;
                        } else if (mButtonBacklightMode.equals(GravityBoxSettings.BB_MODE_ALWAYS_ON)) {
                            if (mPm.isScreenOn() && ((Integer)param.args[0] == 0)) {
                                if (DEBUG) log("Button backlight always on and screen is on. Turning on");
                                param.args[0] = 0xff6e6e6e;
                                return;
                            }
                        }
                    }

                    if (mButtonBacklightNotif) {
                        int color = -1;
                        if (mPendingNotif && mPm.isScreenOn()) {
                            mPendingNotif = false;
                            log("Notification pending and screen is on. Canceling pending notification.");
                            if (!mButtonBacklightMode.equals(GravityBoxSettings.BB_MODE_ALWAYS_ON)) {
                                log("Turning off button backlight");
                                color = 0;
                            }
                        } else if (id == LIGHT_ID_NOTIFICATIONS || id == LIGHT_ID_ATTENTION) {
                            if ((Integer)param.args[0] != 0 && !mPm.isScreenOn()) {
                                mPendingNotif = true;
                                log("New notification and screen is off. Turning on button backlight");
                                color = (Integer)param.args[0];
                            } else {
                                mPendingNotif = false;
                                log("Notification dismissed or screen on");
                                if (!mPm.isScreenOn() ||
                                        !mButtonBacklightMode.equals(GravityBoxSettings.BB_MODE_ALWAYS_ON)) {
                                    color = 0;
                                    log("Turning off button backlight");
                                }
                            }
                        }
                        if (color != -1) {
                            Object ls = XposedHelpers.getSurroundingThis(param.thisObject);
                            int np = XposedHelpers.getIntField(ls, "mNativePointer");
                            XposedHelpers.callMethod(ls, "setLight_native",
                                    np, LIGHT_ID_BUTTONS, color, 0, 0, 0, 0);
                        }
                    }
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