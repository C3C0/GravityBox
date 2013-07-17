package com.ceco.gm2.gravitybox;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModPhone {
    private static final String TAG = "ModPhone";
    public static final String PACKAGE_NAME = "com.android.phone";
    private static final String CLASS_IN_CALL_SCREEN = "com.android.phone.InCallScreen";
    private static final String ENUM_PHONE_STATE = "com.android.internal.telephony.PhoneConstants$State";
    private static final String CLASS_ASYNC_RESULT = "android.os.AsyncResult";
    private static final boolean DEBUG = false;

    private static SensorManager mSensorManager;
    private static boolean mSensorListenerAttached = false;
    private static int mFlipAction = GravityBoxSettings.PHONE_FLIP_ACTION_NONE;
    private static Object mInCallScreen;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static PhoneSensorEventListener mPhoneSensorEventListener = 
            new PhoneSensorEventListener(new PhoneSensorEventListener.ActionHandler() {

                @Override
                public void onFaceUp() {
                    log("PhoneSensorEventListener.onFaceUp");
                    // do nothing
                }

                @Override
                public void onFaceDown() {
                    log("PhoneSensorEventListener.onFaceDown");
                    if (mInCallScreen == null) return;

                    try {
                        switch (mFlipAction) {
                            case GravityBoxSettings.PHONE_FLIP_ACTION_MUTE:
                                log("Muting call");
                                XposedHelpers.callMethod(mInCallScreen, "internalSilenceRinger");
                                break;
                            case GravityBoxSettings.PHONE_FLIP_ACTION_DISMISS:
                                log("Dismissing call");
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

        log("Sensor listener attached");
    }

    private static void detachSensorListener() {
        if (mSensorManager == null || !mSensorListenerAttached) return;

        mSensorManager.unregisterListener(mPhoneSensorEventListener);
        mSensorListenerAttached = false;

        log("Sensor listener detached");
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classInCallScreen = XposedHelpers.findClass(CLASS_IN_CALL_SCREEN, classLoader);
            final Class<? extends Enum> enumPhoneState = (Class<? extends Enum>) Class.forName(ENUM_PHONE_STATE);

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
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}