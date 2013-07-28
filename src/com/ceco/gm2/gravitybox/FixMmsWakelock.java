package com.ceco.gm2.gravitybox;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixMmsWakelock {
    public static final String PACKAGE_NAME = "com.android.mms";
    private static final String TAG = "FixMmsWakelock";
    private static final String CLASS_MMS_RECEIVER = "com.android.mms.transaction.MmsSystemEventReceiver";
    private static final String CLASS_SMS_RECEIVER = "com.android.mms.transaction.SmsReceiver";
    private static final String CLASS_CB_MNOTIF = "com.android.mms.transaction.CBMessagingNotification";
    private static final String CLASS_MNOTIF = "com.android.mms.transaction.MessagingNotification";
    private static final String CLASS_NOTIF_PROFILE = "com.android.mms.transaction.MessagingNotification$NotificationProfile";

    private static Unhook mSmsPmHook = null;
    private static Unhook mMmsPmHook = null;
    private static Unhook mCbPmHook = null;
    private static Unhook mMnotifPmHook1 = null;
    private static Unhook mMnotifPmHook2 = null;
    private static Unhook mMnotifPmHook3 = null;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> mmsReceiverClass = XposedHelpers.findClass(CLASS_MMS_RECEIVER, classLoader);
            XposedHelpers.findAndHookMethod(mmsReceiverClass,
                    "onReceive", Context.class, Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    log("MmsReceiver onReceive ENTERED");
                    Context context = (Context) param.args[0];
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    mMmsPmHook = XposedHelpers.findAndHookMethod(
                            pm.getClass(), "newWakeLock", int.class, String.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    log("PowerManager newWakeLock called from MMS app - setting partial wakelock");
                                    param.args[0] = 1;
                                }
                            });
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mMmsPmHook != null) {
                        mMmsPmHook.unhook();
                        mMmsPmHook = null;
                    }
                    log("MmsReceiver onReceive EXITED");
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }

        try {
            final Class<?> smsReceiverClass = XposedHelpers.findClass(CLASS_SMS_RECEIVER, classLoader);
            XposedHelpers.findAndHookMethod(smsReceiverClass,
                    "beginStartingService", Context.class, Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    log("SmsReceiver beginStartingService ENTERED");
                    Context context = (Context) param.args[0];
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    mSmsPmHook = XposedHelpers.findAndHookMethod(
                            pm.getClass(), "newWakeLock", int.class, String.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    log("PowerManager newWakeLock called from MMS app - setting partial wakelock");
                                    param.args[0] = 1;
                                }
                            });
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mSmsPmHook != null) {
                        mSmsPmHook.unhook();
                        mSmsPmHook = null;
                    }
                    log("smsReceiver beginStartingService EXITED");
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }

        try {
            final Class<?> cbMnotifClass = XposedHelpers.findClass(CLASS_CB_MNOTIF, classLoader);
            XposedHelpers.findAndHookMethod(cbMnotifClass,
                    "updateNotification", Context.class, Intent.class, String.class, 
                    int.class, boolean.class, CharSequence.class, long.class, 
                    String.class, int.class, int.class, Uri.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    log("CBMessagingNotification updateNotification ENTERED");
                    Context context = (Context) param.args[0];
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    mCbPmHook = XposedHelpers.findAndHookMethod(
                            pm.getClass(), "newWakeLock", int.class, String.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    log("PowerManager newWakeLock called from MMS app - setting partial wakelock");
                                    param.args[0] = 1;
                                }
                            });
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mCbPmHook != null) {
                        mCbPmHook.unhook();
                        mCbPmHook = null;
                    }
                    log("CBMessagingNotification updateNotification EXITED");
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }

        try {
            final Class<?> mnotifClass = XposedHelpers.findClass(CLASS_MNOTIF, classLoader);
            XposedHelpers.findAndHookMethod(mnotifClass,
                    "notifyClassZeroMessage", Context.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    log("MessagingNotification notifyClassZeroMessage ENTERED");
                    Context context = (Context) param.args[0];
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    mMnotifPmHook1 = XposedHelpers.findAndHookMethod(
                            pm.getClass(), "newWakeLock", int.class, String.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    log("PowerManager newWakeLock called from MMS app - setting partial wakelock");
                                    param.args[0] = 1;
                                }
                            });
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mMnotifPmHook1 != null) {
                        mMnotifPmHook1.unhook();
                        mMnotifPmHook1 = null;
                    }
                    log("MessagingNotification notifyClassZeroMessage EXITED");
                }
            });

            XposedHelpers.findAndHookMethod(mnotifClass,
                    "notifyFailed", Context.class, boolean.class, long.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    log("MessagingNotification notifyFailed ENTERED");
                    Context context = (Context) param.args[0];
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    mMnotifPmHook2 = XposedHelpers.findAndHookMethod(
                            pm.getClass(), "newWakeLock", int.class, String.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    log("PowerManager newWakeLock called from MMS app - setting partial wakelock");
                                    param.args[0] = 1;
                                }
                            });
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mMnotifPmHook2 != null) {
                        mMnotifPmHook2.unhook();
                        mMnotifPmHook2 = null;
                    }
                    log("MessagingNotification notifyFailed EXITED");
                }
            });

            XposedHelpers.findAndHookMethod(mnotifClass,
                    "updateNotification", Context.class, boolean.class, int.class, long.class, 
                    CLASS_NOTIF_PROFILE, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    log("MessagingNotification updateNotification ENTERED");
                    Context context = (Context) param.args[0];
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    mMnotifPmHook3 = XposedHelpers.findAndHookMethod(
                            pm.getClass(), "newWakeLock", int.class, String.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    log("PowerManager newWakeLock called from MMS app - setting partial wakelock");
                                    param.args[0] = 1;
                                }
                            });
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mMnotifPmHook3 != null) {
                        mMnotifPmHook3.unhook();
                        mMnotifPmHook3 = null;
                    }
                    log("MessagingNotification updateNotification EXITED");
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}