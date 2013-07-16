package com.ceco.gm2.gravitybox;

import java.util.List;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Process;
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
    private static final boolean DEBUG = true;

    private static Class<?> classActivityManagerNative;
    private static Object mPhoneWindowManager;
    private static Context mContext;
    private static Context mGbContext;
    private static String mStrAppKilled;
    private static boolean mIsMenuLongPressed = false;
    private static boolean mIsMenuDoubleTap = false;
    private static boolean mIsBackLongPressed = false;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initZygote(final XSharedPreferences prefs) {
        try {
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
                    log("Phone window manager initialized");
                }
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeDispatching", 
                    CLASS_WINDOW_STATE, KeyEvent.class, int.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if ((Boolean) XposedHelpers.callMethod(mPhoneWindowManager, "keyguardOn")) return;

                    prefs.reload();
                    KeyEvent event = (KeyEvent) param.args[1];
                    int keyCode = event.getKeyCode();
                    boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
                    Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");

                    if (keyCode == KeyEvent.KEYCODE_MENU) {
                        if (!down) {
                            mHandler.removeCallbacks(mMenuLongPress);
                            if (mIsMenuLongPressed) {
                                param.setResult(-1);
                                return;
                            }
                        } else {
                            if (event.getRepeatCount() == 0) {
                                if (mIsMenuDoubleTap) {
                                    performMenuDoubleTapAction();
                                    mHandler.removeCallbacks(mMenuDoubleTapReset);
                                    mIsMenuDoubleTap = false;
                                } else {
                                    mIsMenuLongPressed = false;
                                    mIsMenuDoubleTap = true;
                                    mHandler.postDelayed(mMenuLongPress, ViewConfiguration.getLongPressTimeout());
                                    mHandler.postDelayed(mMenuDoubleTapReset, 500);
                                }
                            } else {
                                param.setResult(-1);
                                return;
                            }
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (!down) {
                            mHandler.removeCallbacks(mBackLongPress);
                            if (mIsBackLongPressed) {
                                param.setResult(-1);
                                return;
                            }
                        } else {
                            if (event.getRepeatCount() == 0) {
                                mIsBackLongPressed = false;
                                mHandler.postDelayed(mBackLongPress, ViewConfiguration.getLongPressTimeout());
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
        }
    };

    private static Runnable mMenuDoubleTapReset = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("menu key double tap timed out");
            mIsMenuDoubleTap = false;
        }
    };

    private static void performMenuDoubleTapAction() {
        if (DEBUG) log("performing menu key double tap action");
    }

    private static Runnable mBackLongPress = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mBackLongPress runnable launched");
            mIsBackLongPressed = true;
        }
    };

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
            }
        } catch (Exception e) {  
            XposedBridge.log(e);  
        }  
    }
}
