package com.ceco.gm2.gravitybox;

import java.util.ArrayList;

import com.ceco.gm2.gravitybox.quicksettings.AQuickSettingsTile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModQuickSettings {
    private static final String TAG = "ModQuickSettings";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_QUICK_SETTINGS = "com.android.systemui.statusbar.phone.QuickSettings";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_PANEL_BAR = "com.android.systemui.statusbar.phone.PanelBar";
    private static final boolean DEBUG = true;

    private static XSharedPreferences mPrefs;
    private static Context mContext;
    private static Context mGbContext;
    private static ViewGroup mContainerView;
    private static Object mPanelBar;
    private static Object mStatusBar;

    private static ArrayList<AQuickSettingsTile> mTiles;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        log("init");

        try {
            final Class<?> quickSettingsClass = XposedHelpers.findClass(CLASS_QUICK_SETTINGS, classLoader);
            final Class<?> phoneStatusBarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> panelBarClass = XposedHelpers.findClass(CLASS_PANEL_BAR, classLoader);

            XposedBridge.hookAllConstructors(quickSettingsClass, quickSettingsConstructHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "setBar", 
                    panelBarClass, quickSettingsSetBarHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "setService", 
                    phoneStatusBarClass, quickSettingsSetServiceHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "addSystemTiles", 
                    ViewGroup.class, LayoutInflater.class, quickSettingsAddSystemTilesHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "updateResources", quickSettingsUpdateResourcesHook);

        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static XC_MethodHook quickSettingsConstructHook = new XC_MethodHook() {

        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            log("QuickSettings constructed - initializing local members");

            mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            mGbContext.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            mContainerView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mContainerView");
        }
    };

    private static XC_MethodHook quickSettingsSetBarHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            mPanelBar = param.args[0];
            log("mPanelBar set");
        }
    };

    private static XC_MethodHook quickSettingsSetServiceHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            mStatusBar = param.args[0];
            log("mStatusBar set");
        }
    };

    private static XC_MethodHook quickSettingsAddSystemTilesHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            log("about to add tiles");

            LayoutInflater inflater = (LayoutInflater) param.args[1];

            mTiles = new ArrayList<AQuickSettingsTile>();
        }
    };

    private static XC_MethodHook quickSettingsUpdateResourcesHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
            if (DEBUG) {
                log("updateResources - updating all tiles");
            }

            for (AQuickSettingsTile t : mTiles) {
                t.updateResources();
            }
        }
    };
}