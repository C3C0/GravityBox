package com.ceco.gm2.gravitybox;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.widget.Toast;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModHwKeys {
    private static final String TAG = "ModHwKeys";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_ACTIVITY_MANAGER_NATIVE = "android.app.ActivityManagerNative";
    private static final String CLASS_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";
    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
    private static final boolean DEBUG = false;

    private static Class<?> classActivityManagerNative;
    private static Object mPhoneWindowManager;
    private static Context mContext;
    private static Context mGbContext;
    private static String mStrAppKilled;
    private static String mStrNothingToKill;
    private static String mStrNoPrevApp;
    private static boolean mIsMenuLongPressed = false;
    private static boolean mIsMenuDoubleTap = false;
    private static boolean mIsBackLongPressed = false;
    private static int mMenuLongpressAction = 0;
    private static int mMenuDoubletapAction = 0;
    private static int mBackLongpressAction = 0;
    private static int mDoubletapSpeed = GravityBoxSettings.HWKEY_DOUBLETAP_SPEED_DEFAULT;
    private static int mKillDelay = GravityBoxSettings.HWKEY_KILL_DELAY_DEFAULT;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static enum HwKey {
        MENU,
        BACK
    }

    private static enum HwKeyTrigger {
        MENU_LONGPRESS,
        MENU_DOUBLETAP,
        BACK_LONGPRESS
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            log("Broadcast received: " + intent.toString());

            String action = intent.getAction();
            int value = GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE)) {
                value = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, value);
            }

            if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED)) {
                mMenuLongpressAction = value;
                log("Menu long-press action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED)) {
                mMenuDoubletapAction = value;
                log("Menu double-tap action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED)) {
                mBackLongpressAction = value;
                log("Back long-press action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED)) {
                mDoubletapSpeed = value;
                log("Doubletap speed set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_KILL_DELAY_CHANGED)) {
                mKillDelay = value;
                log("Kill delay set to: " + value);
            }
        }
    };

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            try {
                mMenuLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_MENU_LONGPRESS, "0"));
                mMenuDoubletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_MENU_DOUBLETAP, "0"));
                mBackLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_BACK_LONGPRESS, "0"));
                mDoubletapSpeed = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_DOUBLETAP_SPEED, "400"));
                mKillDelay = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_KILL_DELAY, "1000"));
            } catch (NumberFormatException e) {
                XposedBridge.log(e);
            }

            final Class<?> classPhoneWindowManager = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);
            classActivityManagerNative = XposedHelpers.findClass(CLASS_ACTIVITY_MANAGER_NATIVE, null);

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init",
                    Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mPhoneWindowManager = param.thisObject;
                    mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
                    mGbContext = mContext.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);

                    Resources res = mGbContext.getResources();
                    mStrAppKilled = res.getString(res.getIdentifier("app_killed", "string", GravityBox.PACKAGE_NAME));
                    mStrNothingToKill = res.getString(res.getIdentifier("nothing_to_kill", "string", GravityBox.PACKAGE_NAME));
                    mStrNoPrevApp = res.getString(res.getIdentifier("no_previous_app_found", "string", GravityBox.PACKAGE_NAME));

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_KILL_DELAY_CHANGED);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);

                    log("Phone window manager initialized");
                }
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeDispatching", 
                    CLASS_WINDOW_STATE, KeyEvent.class, int.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if ((Boolean) XposedHelpers.callMethod(mPhoneWindowManager, "keyguardOn")) return;

                    KeyEvent event = (KeyEvent) param.args[1];
                    int keyCode = event.getKeyCode();
                    boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
                    Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");

                    if (keyCode == KeyEvent.KEYCODE_MENU) {
                        if (!hasAction(HwKey.MENU)) return;

                        if (!down) {
                            mHandler.removeCallbacks(mMenuLongPress);
                            if (mIsMenuLongPressed) {
                                param.setResult(-1);
                                return;
                            }
                        } else {
                            if (event.getRepeatCount() == 0) {
                                if (mIsMenuDoubleTap) {
                                    performAction(HwKeyTrigger.MENU_DOUBLETAP);
                                    mHandler.removeCallbacks(mMenuDoubleTapReset);
                                    mIsMenuDoubleTap = false;
                                } else {
                                    mIsMenuLongPressed = false;
                                    mIsMenuDoubleTap = true;
                                    mHandler.postDelayed(mMenuLongPress, getLongpressTimeoutForAction(mMenuLongpressAction));
                                    mHandler.postDelayed(mMenuDoubleTapReset, mDoubletapSpeed);
                                }
                            } else {
                                param.setResult(-1);
                                return;
                            }
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (!hasAction(HwKey.BACK)) return;

                        if (!down) {
                            mHandler.removeCallbacks(mBackLongPress);
                            if (mIsBackLongPressed) {
                                param.setResult(-1);
                                return;
                            }
                        } else {
                            if (event.getRepeatCount() == 0) {
                                mIsBackLongPressed = false;
                                mHandler.postDelayed(mBackLongPress, getLongpressTimeoutForAction(mBackLongpressAction));
                            } else {
                                param.setResult(-1);
                                return;
                            }
                        }
                    }
                }
            });

        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static Runnable mMenuLongPress = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mMenuLongPress runnable launched");
            mIsMenuLongPressed = true;
            performAction(HwKeyTrigger.MENU_LONGPRESS);
        }
    };

    private static Runnable mMenuDoubleTapReset = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("menu key double tap timed out");
            mIsMenuDoubleTap = false;
        }
    };

    private static Runnable mBackLongPress = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mBackLongPress runnable launched");
            mIsBackLongPressed = true;
            performAction(HwKeyTrigger.BACK_LONGPRESS);
        }
    };

    private static int getActionForHwKeyTrigger(HwKeyTrigger keyTrigger) {
        int action = GravityBoxSettings.HWKEY_ACTION_DEFAULT;

            if (keyTrigger == HwKeyTrigger.MENU_LONGPRESS) {
                action = mMenuLongpressAction;
            } else if (keyTrigger == HwKeyTrigger.MENU_DOUBLETAP) {
                action = mMenuDoubletapAction;
            } else if (keyTrigger == HwKeyTrigger.BACK_LONGPRESS) {
                action = mBackLongpressAction;
            }

        if (DEBUG) log("Action for HWKEY trigger " + keyTrigger + " = " + action);
        return action;
    }

    private static boolean hasAction(HwKey key) {
        boolean retVal = false;
        if (key == HwKey.MENU) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.MENU_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.MENU_DOUBLETAP) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        } else if (key == HwKey.BACK) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.BACK_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        }

        if (DEBUG) log("HWKEY " + key + " has action = " + retVal);
        return retVal;
    }

    private static int getLongpressTimeoutForAction(int action) {
        return (action == GravityBoxSettings.HWKEY_ACTION_KILL) ?
                mKillDelay : ViewConfiguration.getLongPressTimeout();
    }

    private static void performAction(HwKeyTrigger keyTrigger) {
        int action = getActionForHwKeyTrigger(keyTrigger);
        if (DEBUG) log("Performing action " + action + " for HWKEY trigger " + keyTrigger.toString());

        if (action == GravityBoxSettings.HWKEY_ACTION_DEFAULT) return;

        if (action == GravityBoxSettings.HWKEY_ACTION_SEARCH) {
            launchSearchActivity();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_VOICE_SEARCH) {
            launchVoiceSearchActivity();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_PREV_APP) {
            switchToLastApp();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_KILL) {
            killForegroundApp();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_SLEEP) {
            goToSleep();
        }
    }

    private static void launchSearchActivity() {
        try {
            XposedHelpers.callMethod(mPhoneWindowManager, "launchAssistAction");
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void launchVoiceSearchActivity() {
        try {
            XposedHelpers.callMethod(mPhoneWindowManager, "launchAssistLongPressAction");
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void killForegroundApp() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        final Intent intent = new Intent(Intent.ACTION_MAIN);
                        String defaultHomePackage = "com.android.launcher";
                        intent.addCategory(Intent.CATEGORY_HOME);
                        final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
                        if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                            defaultHomePackage = res.activityInfo.packageName;
                        }
        
                        Object mgr = XposedHelpers.callStaticMethod(classActivityManagerNative, "getDefault");
        
                        @SuppressWarnings("unchecked")
                        List<RunningAppProcessInfo> apps = (List<RunningAppProcessInfo>) 
                                XposedHelpers.callMethod(mgr, "getRunningAppProcesses");
        
                        boolean targetKilled = false;
                        for (RunningAppProcessInfo appInfo : apps) {  
                            int uid = appInfo.uid;  
                            // Make sure it's a foreground user application (not system,  
                            // root, phone, etc.)  
                            if (uid >= Process.FIRST_APPLICATION_UID && uid <= Process.LAST_APPLICATION_UID  
                                    && appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                                    !appInfo.processName.equals("com.android.systemui") &&
                                    !appInfo.processName.equals("com.mediatek.bluetooth") &&
                                    !appInfo.processName.equals(defaultHomePackage)) {  
                                log("Killing process ID " + appInfo.pid + ": " + appInfo.processName);
                                Process.killProcess(appInfo.pid);
                                targetKilled = true;
                                break;
                            }  
                        }
        
                        if (targetKilled) {
                            Class<?>[] paramArgs = new Class<?>[3];
                            paramArgs[0] = XposedHelpers.findClass(CLASS_WINDOW_STATE, null);
                            paramArgs[1] = int.class;
                            paramArgs[2] = boolean.class;
                            XposedHelpers.callMethod(mPhoneWindowManager, "performHapticFeedbackLw",
                                    paramArgs, null, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING, true);
                            Toast.makeText(mContext, mStrAppKilled, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, mStrNothingToKill, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {  
                        XposedBridge.log(e);  
                    }
                }
            }
         );
    }

    private static void switchToLastApp() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(
            new Runnable() {
                @Override
                public void run() {
                    int lastAppId = 0;
                    int looper = 1;
                    String packageName;
                    final Intent intent = new Intent(Intent.ACTION_MAIN);
                    final ActivityManager am = (ActivityManager) mContext
                            .getSystemService(Context.ACTIVITY_SERVICE);
                    String defaultHomePackage = "com.android.launcher";
                    intent.addCategory(Intent.CATEGORY_HOME);
                    final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
                    if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                        defaultHomePackage = res.activityInfo.packageName;
                    }
                    List <ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(5);
                    // lets get enough tasks to find something to switch to
                    // Note, we'll only get as many as the system currently has - up to 5
                    while ((lastAppId == 0) && (looper < tasks.size())) {
                        packageName = tasks.get(looper).topActivity.getPackageName();
                        if (!packageName.equals(defaultHomePackage) && !packageName.equals("com.android.systemui")) {
                            lastAppId = tasks.get(looper).id;
                        }
                        looper++;
                    }
                    if (lastAppId != 0) {
                        am.moveTaskToFront(lastAppId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
                    } else {
                        Toast.makeText(mContext, mStrNoPrevApp, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    private static void goToSleep() {
        try {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            pm.goToSleep(SystemClock.uptimeMillis());
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}