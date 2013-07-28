package com.ceco.gm2.gravitybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModStatusbarColor {
    private static final String TAG = "ModStatusbarColor";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_PANEL_BAR = "com.android.systemui.statusbar.phone.PanelBar";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";

    private static View mPanelBar;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            log("received broadcast: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_BGCOLOR_CHANGED) &&
                            intent.hasExtra(GravityBoxSettings.EXTRA_SB_BGCOLOR)) {
                int bgColor = intent.getIntExtra(GravityBoxSettings.EXTRA_SB_BGCOLOR, Color.BLACK);
                setStatusbarBgColor(bgColor);
            }
        }
    };

    public static void initZygote() {
        log("initZygote");

        try {
            final Class<?> phoneWindowManagerClass = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);

            log("replacing getSystemDecorRectLw method");
            XposedHelpers.findAndHookMethod(phoneWindowManagerClass,
                    "getSystemDecorRectLw", Rect.class, new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Rect rect = (Rect) param.args[0];
                            rect.left = XposedHelpers.getIntField(param.thisObject, "mSystemLeft");
                            rect.top = XposedHelpers.getIntField(param.thisObject, "mSystemTop");
                            rect.right = XposedHelpers.getIntField(param.thisObject, "mSystemRight");
                            rect.bottom = XposedHelpers.getIntField(param.thisObject, "mSystemBottom");
                            return 0;
                        }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        log("init");

        try {
            final Class<?> panelBarClass = XposedHelpers.findClass(CLASS_PANEL_BAR, classLoader);
            final Class<?> phoneStatusbarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);

            XposedBridge.hookAllConstructors(panelBarClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPanelBar = (View) param.thisObject;

                    IntentFilter intentFilter = new IntentFilter(
                            GravityBoxSettings.ACTION_PREF_STATUSBAR_BGCOLOR_CHANGED);
                    mPanelBar.getContext().registerReceiver(mBroadcastReceiver, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "makeStatusBarView", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    int bgColor = prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_BGCOLOR, Color.BLACK);
                    setStatusbarBgColor(bgColor);
                }
            });

        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void setStatusbarBgColor(int color) {
        if (mPanelBar == null) return;

        ColorDrawable colorDrawable = new ColorDrawable();
        colorDrawable.setColor(color);
        mPanelBar.setBackground(colorDrawable);
        log("statusbar background color set to: " + color);
    }
}