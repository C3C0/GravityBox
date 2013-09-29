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

import java.lang.reflect.Method;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModPhone {
    private static final String TAG = "GB:ModPhone";
    public static final String PACKAGE_NAME = "com.android.phone";
    private static final String CLASS_IN_CALL_SCREEN = "com.android.phone.InCallScreen";
    private static final String ENUM_PHONE_STATE = Build.VERSION.SDK_INT > 16 ?
            "com.android.internal.telephony.PhoneConstants$State" :
            "com.android.internal.telephony.Phone$State";
    private static final String CLASS_ASYNC_RESULT = "android.os.AsyncResult";
    private static final String CLASS_CALL_NOTIFIER = "com.android.phone.CallNotifier";
    private static final String CLASS_SERVICE_STATE_EXT = "com.mediatek.op.telephony.ServiceStateExt";
    private static final String CLASS_GSM_SERVICE_STATE_TRACKER = 
            "com.android.internal.telephony.gsm.GsmServiceStateTracker";
    private static final boolean DEBUG = false;

    private static SensorManager mSensorManager;
    private static boolean mSensorListenerAttached = false;
    private static int mFlipAction = GravityBoxSettings.PHONE_FLIP_ACTION_NONE;
    private static Object mInCallScreen;
    private static Unhook mVibrateHook;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static PhoneSensorEventListener mPhoneSensorEventListener = 
            new PhoneSensorEventListener(new PhoneSensorEventListener.ActionHandler() {

                @Override
                public void onFaceUp() {
                    if (DEBUG) log("PhoneSensorEventListener.onFaceUp");
                    // do nothing
                }

                @Override
                public void onFaceDown() {
                    if (DEBUG) log("PhoneSensorEventListener.onFaceDown");
                    if (mInCallScreen == null) return;

                    try {
                        switch (mFlipAction) {
                            case GravityBoxSettings.PHONE_FLIP_ACTION_MUTE:
                                if (DEBUG) log("Muting call");
                                XposedHelpers.callMethod(mInCallScreen, "internalSilenceRinger");
                                break;
                            case GravityBoxSettings.PHONE_FLIP_ACTION_DISMISS:
                                if (DEBUG) log("Dismissing call");
                                XposedHelpers.callMethod(mInCallScreen, "internalHangup");
                                break;
                            case GravityBoxSettings.PHONE_FLIP_ACTION_NONE:
                            default:
                                // do nothing
                        }
                    } catch (Exception e) {
                        XposedBridge.log(e);
                    }
                }
            });

    private static void attachSensorListener() {
        if (mSensorManager == null || 
                mSensorListenerAttached ||
                mFlipAction == GravityBoxSettings.PHONE_FLIP_ACTION_NONE) return;

        mPhoneSensorEventListener.reset();
        mSensorManager.registerListener(mPhoneSensorEventListener, 
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorListenerAttached = true;

        if (DEBUG) log("Sensor listener attached");
    }

    private static void detachSensorListener() {
        if (mSensorManager == null || !mSensorListenerAttached) return;

        mSensorManager.unregisterListener(mPhoneSensorEventListener);
        mSensorListenerAttached = false;

        if (DEBUG) log("Sensor listener detached");
    }

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            if (Utils.hasGeminiSupport()) {
                final Class<?> classServiceStateExt = XposedHelpers.findClass(CLASS_SERVICE_STATE_EXT, null);

                XposedHelpers.findAndHookMethod(classServiceStateExt, "ignoreDomesticRoaming", 
                        new XC_MethodReplacement() {

                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        prefs.reload();
                        boolean mvno = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NATIONAL_ROAMING, false);
                        if (DEBUG) log("ignoreDomesticRoaming: " + mvno);
                        return mvno;
                    }
                });
            } else {
                final Class<?> classGsmServiceStateTracker = XposedHelpers.findClass(
                        CLASS_GSM_SERVICE_STATE_TRACKER, null);

                XposedHelpers.findAndHookMethod(classGsmServiceStateTracker, "isRoamingBetweenOperators", 
                        boolean.class, "android.telephony.ServiceState", new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        prefs.reload();
                        boolean mvno = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NATIONAL_ROAMING, false);
                        final Class<?> classSystemProperties = 
                                XposedHelpers.findClass("android.os.SystemProperties", null);
                        String simNumeric = (String) XposedHelpers.callStaticMethod(
                                classSystemProperties, "get", "gsm.sim.operator.numeric", "");
                        String operatorNumeric = (String) XposedHelpers.callMethod(
                                param.args[1], "getOperatorNumeric");
                        boolean equalsMcc = true;
                        try {
                            equalsMcc = simNumeric.substring(0, 3).
                                    equals(operatorNumeric.substring(0, 3));
                        } catch (Exception e) { }

                        boolean result = (Boolean) param.getResult();
                        result = result && !(equalsMcc && mvno);
                        if (DEBUG) log("isRoamingBetweenOperators: " + result);
                        param.setResult(result);
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classInCallScreen = XposedHelpers.findClass(CLASS_IN_CALL_SCREEN, classLoader);
            final Class<? extends Enum> enumPhoneState = (Class<? extends Enum>) Class.forName(ENUM_PHONE_STATE);
            final Class<?> classCallNotifier = XposedHelpers.findClass(CLASS_CALL_NOTIFIER, classLoader);

            XposedHelpers.findAndHookMethod(classInCallScreen, "onCreate", Bundle.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mInCallScreen = param.thisObject;
                    Context context = (Context) mInCallScreen;
                    if (context == null) {
                        log("Context is null. Skipping...");
                        return;
                    }

                    mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                }
            });

            XposedHelpers.findAndHookMethod(classInCallScreen, 
                    "onPhoneStateChanged", CLASS_ASYNC_RESULT, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object mCM = XposedHelpers.getObjectField(param.thisObject, "mCM");
                    if (mCM == null) return;

                    if (XposedHelpers.callMethod(mCM, "getState") == Enum.valueOf(enumPhoneState, "RINGING")) {
                        if (DEBUG) log("PHONE_STATE is RINGING - attaching sensor listener (if not attached yet)");

                        prefs.reload();
                        mFlipAction = GravityBoxSettings.PHONE_FLIP_ACTION_NONE;
                        try {
                            mFlipAction = Integer.valueOf(prefs.getString(
                                    GravityBoxSettings.PREF_KEY_PHONE_FLIP, "0"));
                            if (DEBUG) log("mFlipAction = " + mFlipAction);
                        } catch (NumberFormatException e) {
                            XposedBridge.log(e);
                        }

                        attachSensorListener();
                    } else {
                        if (DEBUG) log("PHONE_STATE is NOT RINGING - detaching sensor listener (if is attached)");
                        detachSensorListener();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classInCallScreen, "internalSilenceRinger", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (DEBUG) log("internalSilenceRinger - detaching sensor listener (if is attached)");
                    detachSensorListener();
                }
            });

            XposedHelpers.findAndHookMethod(classCallNotifier, 
                    "onPhoneStateChanged", CLASS_ASYNC_RESULT, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (DEBUG ) log("CallNotifier: onPhoneStateChanged ENTERED");

                    prefs.reload();
                    if (!prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_PHONE_CALL_CONNECT_VIBRATE_DISABLE, false)) {
                        return;
                    }

                    Object app = XposedHelpers.getObjectField(param.thisObject, "mApplication");
                    if (app == null) return;

                    Method m = app.getClass().getMethod("getSystemService", String.class);
                    Vibrator v = (Vibrator) m.invoke(app, "vibrator");
                    if (v == null) return;

                    mVibrateHook = XposedHelpers.findAndHookMethod(
                            v.getClass(), "vibrate", long.class, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (DEBUG) log("Vibrator: vibrate called from CallNotifier - ignoring");
                            param.setResult(null);
                        }
                    });
                    if (DEBUG) log("CallNotifier: Vibrator.vibrate() method hooked");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mVibrateHook != null) {
                        if (DEBUG) log("CallNotifier: unhooking vibrate hook");
                        mVibrateHook.unhook();
                        mVibrateHook = null;
                    }

                    if (DEBUG ) log("CallNotifier: onPhoneStateChanged EXITED");
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}