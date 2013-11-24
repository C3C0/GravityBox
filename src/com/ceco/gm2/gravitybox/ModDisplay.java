/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.gm2.gravitybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XResources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.ResultReceiver;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModDisplay {
    private static final String TAG = "GB:ModDisplay";
    private static final String CLASS_DISPLAY_POWER_CONTROLLER = "com.android.server.power.DisplayPowerController";
    private static final String CLASS_LIGHT_SERVICE_LIGHT = "com.android.server.LightsService$Light";
    private static final String CLASS_LIGHT_SERVICE = "com.android.server.LightsService";
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

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
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
                    updateButtonBacklight();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BB_NOTIF) && Build.VERSION.SDK_INT < 19) {
                    mButtonBacklightNotif = intent.getBooleanExtra(GravityBoxSettings.EXTRA_BB_NOTIF, false);
                    if (!mButtonBacklightNotif) {
                        mPendingNotif = false;
                        updateButtonBacklight();
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)
                        || intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                updateButtonBacklight(intent.getAction().equals(Intent.ACTION_SCREEN_ON));
            }
        }
    };

    private static void updateButtonBacklight() {
        updateButtonBacklight(true);
    }

    private static void updateButtonBacklight(boolean isScreenOn) {
        if (mLight == null || mPendingNotif) return;

        try {
            Integer color = null;
            if (mButtonBacklightMode.equals(GravityBoxSettings.BB_MODE_ALWAYS_ON)) {
                color = isScreenOn ? 0xff6e6e6e : 0;
            } else if (mButtonBacklightMode.equals(GravityBoxSettings.BB_MODE_DISABLE)) {
                color = 0;
            } else if (!isScreenOn) {
                color = 0;
            }
    
            if (color != null) {
                Object ls = XposedHelpers.getSurroundingThis(mLight);
                int np = XposedHelpers.getIntField(ls, "mNativePointer");
                XposedHelpers.callMethod(ls, "setLight_native",
                        np, LIGHT_ID_BUTTONS, color, 0, 0, 0, 0);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static boolean mPendingNotif = false;
    private static Object mLight;
    private static Handler mHandler;
    private static int mPendingNotifColor = 0;
    private static WakeLock mWakeLock;
    private static Runnable mPendingNotifRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLight == null) return;
            try {
                Object ls = XposedHelpers.getSurroundingThis(mLight);
                int np = XposedHelpers.getIntField(ls, "mNativePointer");
                if (!mPendingNotif) {
                    mHandler.removeCallbacks(this);
                    mPendingNotifColor = 
                            mButtonBacklightMode.equals(GravityBoxSettings.BB_MODE_ALWAYS_ON) 
                                    && mPm.isScreenOn() ? 0xff6e6e6e : 0;
                    XposedHelpers.callMethod(ls, "setLight_native",
                            np, LIGHT_ID_BUTTONS, mPendingNotifColor, 0, 0, 0, 0);
                } else {
                    if (mPendingNotifColor == 0) {
                        mPendingNotifColor = 0xff6e6e6e;
                        XposedHelpers.callMethod(ls, "setLight_native",
                            np, LIGHT_ID_BUTTONS, mPendingNotifColor, 0, 0, 0, 0);
                        mHandler.postDelayed(mPendingNotifRunnable, 500);
                    } else {
                        mPendingNotifColor = 0;
                        XposedHelpers.callMethod(ls, "setLight_native",
                            np, LIGHT_ID_BUTTONS, mPendingNotifColor, 0, 0, 0, 0);
                        mHandler.postDelayed(mPendingNotifRunnable, 3000);
                    }
                }
            } catch(Exception e) {
                XposedBridge.log(e);
            }
        }
    };

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> classDisplayPowerController = Build.VERSION.SDK_INT > 16 ?
                    XposedHelpers.findClass(CLASS_DISPLAY_POWER_CONTROLLER, null) : null;
            final Class<?> classLight = XposedHelpers.findClass(CLASS_LIGHT_SERVICE_LIGHT, null);
            final Class<?> classLightService = XposedHelpers.findClass(CLASS_LIGHT_SERVICE, null);

            final boolean brightnessSettingsEnabled = 
                    prefs.getBoolean(GravityBoxSettings.PREF_KEY_BRIGHTNESS_MASTER_SWITCH, false);

            mButtonBacklightMode = prefs.getString(
                    GravityBoxSettings.PREF_KEY_BUTTON_BACKLIGHT_MODE, GravityBoxSettings.BB_MODE_DEFAULT);
            mButtonBacklightNotif = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS, false) &&
                    Build.VERSION.SDK_INT < 19;
            
            if (brightnessSettingsEnabled && classDisplayPowerController != null) {
                int brightnessMin = prefs.getInt(GravityBoxSettings.PREF_KEY_BRIGHTNESS_MIN, 20);
                XResources.setSystemWideReplacement(
                    "android", "integer", "config_screenBrightnessSettingMinimum", brightnessMin);
                if (DEBUG) log("Minimum brightness value set to: " + brightnessMin);

                int screenDim = prefs.getInt(GravityBoxSettings.PREF_KEY_SCREEN_DIM_LEVEL, 10);
                XResources.setSystemWideReplacement(
                        "android", "integer", "config_screenBrightnessDim", screenDim);
                if (DEBUG) log("Screen dim level set to: " + screenDim);

                XposedBridge.hookAllConstructors(classDisplayPowerController, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        if (DEBUG) log("DisplayPowerController constructed");
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
    
                        if (brightnessSettingsEnabled) {
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
                        }
    
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(ACTION_GET_AUTOBRIGHTNESS_CONFIG);
                        if (brightnessSettingsEnabled) {
                            intentFilter.addAction(ACTION_SET_AUTOBRIGHTNESS_CONFIG);
                        }
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
            }

            XposedBridge.hookAllConstructors(classLightService, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (context != null) {
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BUTTON_BACKLIGHT_CHANGED);
                        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
                        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                        context.registerReceiver(mBroadcastReceiver, intentFilter);
                        if (DEBUG) log("LightsService constructed. Broadcast receiver registered.");
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classLight, "setLightLocked",
                    int.class, int.class, int.class, int.class, int.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mLight == null) mLight = param.thisObject;
                    int id = XposedHelpers.getIntField(param.thisObject, "mId");
                    if (DEBUG) log("lightId=" + id + "; color=" + param.args[0] + 
                            "; mode=" + param.args[1] + "; " + "onMS=" + param.args[2] + 
                            "; offMS=" + param.args[3] + "; bMode=" + param.args[4]);

                    if (mPm == null) {
                        Object lightService = XposedHelpers.getSurroundingThis(param.thisObject);
                        Context context = (Context) XposedHelpers.getObjectField(lightService, "mContext");
                        mPm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    }

                    if (id == LIGHT_ID_BUTTONS && !mPendingNotif) {
                        if (mButtonBacklightMode.equals(GravityBoxSettings.BB_MODE_DISABLE)) {
                            param.args[0] = param.args[1] = param.args[2] = param.args[3] = param.args[4] = 0;
                            if (DEBUG) log("Button backlight disabled. Turning off");
                            return;
                        } else if (mButtonBacklightMode.equals(GravityBoxSettings.BB_MODE_ALWAYS_ON)) {
                            int color = (Integer)param.args[0];
                            if (mPm.isScreenOn() && (color == 0 || color == Color.BLACK)) {
                                if (DEBUG) log("Button backlight always on and screen is on. Turning on");
                                param.args[0] = 0xff6e6e6e;
                                return;
                            }
                        }
                    }

                    if (mButtonBacklightNotif) {
                        if (mHandler == null) mHandler = new Handler();
                        if (id == LIGHT_ID_NOTIFICATIONS || id == LIGHT_ID_ATTENTION) {
                            if ((Integer)param.args[0] != 0) {
                                if (!mPendingNotif) {
                                    if (DEBUG) log("New notification. Entering PendingNotif state");
                                    mPendingNotif = true;
                                    mWakeLock = mPm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GbModDisplay");
                                    mWakeLock.acquire(3600000);
                                    mHandler.removeCallbacks(mPendingNotifRunnable);
                                    mHandler.post(mPendingNotifRunnable);
                                }
                            } else if (mPendingNotif) {
                                if (DEBUG) log("Notification dismissed. Leaving PendingNotif state");
                                mPendingNotif = false;
                                if (mWakeLock.isHeld()) {
                                    mWakeLock.release();
                                }
                                mWakeLock = null;
                            }
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
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

        if (DEBUG) log("Autobrightness config updated");
    }
}