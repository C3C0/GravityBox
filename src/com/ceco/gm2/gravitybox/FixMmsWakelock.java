package com.ceco.gm2.gravitybox;

import android.content.Context;
import android.content.Intent;

import com.ceco.gm2.gravitybox.Utils.MethodState;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixMmsWakelock {
    public static final String PACKAGE_NAME = "com.android.mms";
    private static final String TAG = "FixMmsWakelock";
    private static final String CLASS_POWER_MANAGER = "android.os.PowerManager";
    private static final String CLASS_MMS_RECEIVER = "com.android.mms.transaction.MmsSystemEventReceiver";
    private static final String CLASS_SMS_RECEIVER = "com.android.mms.transaction.SmsReceiver";

    private static ThreadLocal<MethodState> mMmsReceiverMethodState;
    private static ThreadLocal<MethodState> mSmsReceiverMethodState;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> powerManagerClass = XposedHelpers.findClass(CLASS_POWER_MANAGER, null);

            mMmsReceiverMethodState = new ThreadLocal<MethodState>();
            mMmsReceiverMethodState.set(MethodState.UNKNOWN);
            mSmsReceiverMethodState = new ThreadLocal<MethodState>();
            mSmsReceiverMethodState.set(MethodState.UNKNOWN);

            XposedHelpers.findAndHookMethod(powerManagerClass, "newWakeLock", 
                    int.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if ((mMmsReceiverMethodState.get() != null &&
                            mMmsReceiverMethodState.get().equals(MethodState.METHOD_ENTERED)) ||
                        (mSmsReceiverMethodState.get() != null &&
                            mSmsReceiverMethodState.get().equals(MethodState.METHOD_ENTERED))) {
                        log("PowerManager newWakeLock called from Mms app. Setting partial wakelock.");
                        param.args[0] = 1;
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> mmsReceiverClass = XposedHelpers.findClass(CLASS_MMS_RECEIVER, classLoader);
            final Class<?> smsReceiverClass = XposedHelpers.findClass(CLASS_SMS_RECEIVER, classLoader);

            XposedHelpers.findAndHookMethod(mmsReceiverClass,
                    "onReceive", Context.class, Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    log("mmsReceiver onReceive ENTERED");
                    mMmsReceiverMethodState.set(MethodState.METHOD_ENTERED);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    log("mmsReceiver onReceive EXITED");
                    mMmsReceiverMethodState.set(MethodState.METHOD_EXITED);
                }
            });

            XposedHelpers.findAndHookMethod(smsReceiverClass,
                    "beginStartingService", Context.class, Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    log("smsReceiver beginStartingService ENTERED");
                    mSmsReceiverMethodState.set(MethodState.METHOD_ENTERED);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    log("smsReceiver beginStartingService EXITED");
                    mSmsReceiverMethodState.set(MethodState.METHOD_EXITED);
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}