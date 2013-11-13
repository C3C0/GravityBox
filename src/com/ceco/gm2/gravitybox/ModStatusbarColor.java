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

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class ModStatusbarColor {
    private static final String TAG = "GB:ModStatusbarColor";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_PHONE_STATUSBAR_VIEW = "com.android.systemui.statusbar.phone.PhoneStatusBarView";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_SIGNAL_CLUSTER_VIEW = Utils.hasGeminiSupport() ? 
            "com.android.systemui.statusbar.SignalClusterViewGemini" :
            "com.android.systemui.statusbar.SignalClusterView";
    private static final String CLASS_BATTERY_CONTROLLER = "com.android.systemui.statusbar.policy.BatteryController";
    private static final String CLASS_NOTIF_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String CLASS_POLICY_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_STATE = "com.android.server.wm.WindowState";
    private static final String CLASS_WINDOW_MANAGER_SERVICE = "com.android.server.wm.WindowManagerService";
    private static final String CLASS_STATUSBAR_ICON_VIEW = "com.android.systemui.statusbar.StatusBarIconView";
    private static final String CLASS_STATUSBAR_ICON = "com.android.internal.statusbar.StatusBarIcon";
    private static final boolean DEBUG = false;

    public static final String ACTION_PHONE_STATUSBAR_VIEW_MADE = "gravitybox.intent.action.PHONE_STATUSBAR_VIEW_MADE";

    private static View mPanelBar;
    private static StatusBarIconManager mIconManager;
    private static Object mSignalClusterView;
    private static boolean mIconColorEnabled;
    private static boolean mSkipBatteryIcon;
    private static TextView mClock;
    private static CmCircleBattery mCircleBattery;
    private static TextView mPercentage;
    private static ImageView mBattery;
    private static int mBatteryLevel;
    private static boolean mBatteryPlugged;
    private static Object mBatteryController;
    private static FrameLayout mNotificationPanelView;
    private static NotificationWallpaper mNotificationWallpaper;
    private static boolean mRoamingIndicatorsDisabled;
    private static TransparencyManager mTransparencyManager;
    private static TrafficMeter mTrafficMeter;
    private static Context mContextPwm;
    private static int[] mTransparencyValuesPwm = new int[] { 0, 0, 0, 0};
    private static List<BroadcastSubReceiver> mBroadcastSubReceivers;
    private static Unhook mDisplayContentHook;
    private static Object mPhoneWindowManager;
    private static Object mPhoneStatusBar;

    static {
        mIconColorEnabled = false;
        mSkipBatteryIcon = false;
        mBatteryLevel = 0;
        mBatteryPlugged = false;
        mRoamingIndicatorsDisabled = false;
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void setClock(TextView clock) {
        mClock = clock;
    }

    public static void setCircleBattery(CmCircleBattery circleBattery) {
        mCircleBattery = circleBattery;
    }

    public static void setPercentage(TextView percentage) {
        mPercentage = percentage;
    }

    public static void setBattery(ImageView battery) {
        mBattery = battery;
    }

    public static void setTrafficMeter(TrafficMeter trafficMeter) {
        mTrafficMeter = trafficMeter;
    }

    private static BroadcastReceiver mBroadcastReceiverPwm = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("PhoneWindowManager received broadcast: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_SB_LAUNCHER)) {
                    mTransparencyValuesPwm[0] = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_SB_LAUNCHER, 0);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_SB_LOCKSCREEN)) {
                    mTransparencyValuesPwm[1] = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_SB_LOCKSCREEN, 0);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_NB_LAUNCHER)) {
                    mTransparencyValuesPwm[2] = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_NB_LAUNCHER, 0);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_NB_LOCKSCREEN)) {
                    mTransparencyValuesPwm[3] = intent.getIntExtra(GravityBoxSettings.EXTRA_TM_NB_LOCKSCREEN, 0);
                }
            }
        }
    };

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("received broadcast: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED)
                    && mIconManager != null) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_BG_COLOR)) {
                    int bgColor = intent.getIntExtra(GravityBoxSettings.EXTRA_SB_BG_COLOR, Color.BLACK);
                    setStatusbarBgColor(bgColor);
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_ICON_COLOR)) {
                    int iconColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_SB_ICON_COLOR, mIconManager.getDefaultIconColor());
                    mIconManager.setIconColor(iconColor);
                    applyIconColors();
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR)) {
                    int daColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR, 
                            StatusBarIconManager.DEFAULT_DATA_ACTIVITY_COLOR);
                    mIconManager.setDataActivityColor(daColor);
                    applyIconColors();
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_ICON_COLOR_ENABLE)) {
                    mIconColorEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_SB_ICON_COLOR_ENABLE, false);
                    if (DEBUG) log("Icon colors master switch set to: " + mIconColorEnabled);
                    if (!mIconColorEnabled) mIconManager.clearCache();
                    applyIconColors();
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_COLOR_FOLLOW)) {
                    boolean follow = intent.getBooleanExtra(GravityBoxSettings.EXTRA_SB_COLOR_FOLLOW, false);
                    mIconManager.setFollowStockBatteryColor(follow);
                    applyIconColors();
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_COLOR_SKIP_BATTERY)) {
                    mSkipBatteryIcon = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_SB_COLOR_SKIP_BATTERY, false);
                    applyIconColors();
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_SIGNAL_COLOR_MODE)) {
                    mIconManager.setSignalIconMode(
                            intent.getIntExtra(GravityBoxSettings.EXTRA_SB_SIGNAL_COLOR_MODE,
                                    StatusBarIconManager.SI_MODE_GB));
                    applyIconColors();
                }
            }

            if (intent.getAction().equals(GravityBoxSettings.ACTION_NOTIF_BACKGROUND_CHANGED) &&
                    mNotificationWallpaper != null) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_TYPE)) {
                    mNotificationWallpaper.setType(
                            intent.getStringExtra(GravityBoxSettings.EXTRA_BG_TYPE));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_COLOR)) {
                    mNotificationWallpaper.setColor(
                            intent.getIntExtra(GravityBoxSettings.EXTRA_BG_COLOR, Color.BLACK));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_ALPHA)) {
                    mNotificationWallpaper.setAlpha(
                            intent.getIntExtra(GravityBoxSettings.EXTRA_BG_ALPHA, 60));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_COLOR_MODE)) {
                    mNotificationWallpaper.setColorMode(
                            intent.getStringExtra(GravityBoxSettings.EXTRA_BG_COLOR_MODE));
                }
                updateNotificationPanelBackground();
            }

            if (intent.getAction().equals(GravityBoxSettings.ACTION_DISABLE_ROAMING_INDICATORS_CHANGED)) {
                mRoamingIndicatorsDisabled = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_INDICATORS_DISABLED, false);
            }

            for (BroadcastSubReceiver bsr : mBroadcastSubReceivers) {
                bsr.onBroadcastReceived(context, intent);
            }
        }
    };

    // in Zygote hooks
    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> phoneWindowManagerClass = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);
            final Class<?> windowStateClass = XposedHelpers.findClass(CLASS_WINDOW_STATE, null);
            final Class<?> windowManagerServiceClass = XposedHelpers.findClass(CLASS_WINDOW_MANAGER_SERVICE, null);

            if (DEBUG) log("replacing getSystemDecorRectLw method");
            XposedHelpers.findAndHookMethod(phoneWindowManagerClass,
                    "getSystemDecorRectLw", Rect.class, new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    if (mContextPwm == null) {
                        if (DEBUG) log("getSystemDecorRectLw: registering transparency settings receiver");
                        mTransparencyValuesPwm[0] = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_STATUSBAR_LAUNCHER, 0);
                        mTransparencyValuesPwm[1] = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_STATUSBAR_LOCKSCREEN, 0);
                        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_OVERRIDE, false)
                                && prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_ENABLE, false)) {
                            mTransparencyValuesPwm[2] = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_NAVBAR_LAUNCHER, 0);
                            mTransparencyValuesPwm[3] = prefs.getInt(GravityBoxSettings.PREF_KEY_TM_NAVBAR_LOCKSCREEN, 0);
                        }
                        mPhoneWindowManager = param.thisObject;
                        mContextPwm = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                        mContextPwm.registerReceiver(mBroadcastReceiverPwm, intentFilter);
                    }

                    if (!isTransparencyEnabled()) {
                        if (DEBUG) log("getSystemDecorRectLw: calling original method");
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    } else {
                        if (DEBUG) log("getSystemDecorRectLw: overriding original method");
                        Rect rect = (Rect) param.args[0];
                        rect.left = XposedHelpers.getIntField(param.thisObject, "mSystemLeft");
                        rect.top = XposedHelpers.getIntField(param.thisObject, "mSystemTop");
                        rect.right = XposedHelpers.getIntField(param.thisObject, "mSystemRight");
                        rect.bottom = XposedHelpers.getIntField(param.thisObject, "mSystemBottom");
                        return 0;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneWindowManagerClass, "layoutWindowLw",
                    CLASS_POLICY_WINDOW_STATE, WindowManager.LayoutParams.class, 
                    CLASS_POLICY_WINDOW_STATE, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final boolean isDefaultDisplay = Build.VERSION.SDK_INT > 16 ?
                            (Boolean) XposedHelpers.callMethod(param.args[0], "isDefaultDisplay") : true;
                    if (!isNavbarTransparencyEnabled() || !isDefaultDisplay) return; 

                    final WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) param.args[1];
                    final int fl = attrs.flags;
                    final int sysUiFl = (Integer) XposedHelpers.callMethod(param.args[0], "getSystemUiVisibility");
                    final Rect pf = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpParentFrame");
                    final Rect df = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpDisplayFrame");
                    final Rect cf = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpContentFrame");
                    final Rect vf = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpVisibleFrame");
                    final Rect of = Build.VERSION.SDK_INT > 17 ?
                            (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpOverscanFrame") : null;

                    if ((fl & WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN) != 0 || (sysUiFl
                                & (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                           | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)) != 0) {
                        if (attrs.type == WindowManager.LayoutParams.TYPE_WALLPAPER) {
                            pf.top = df.top = cf.top = Build.VERSION.SDK_INT > 17 ?
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenTop") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenTop");
                            pf.bottom = df.bottom = cf.bottom = pf.top + Build.VERSION.SDK_INT > 17 ? 
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenHeight") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenHeight");
                            if (Build.VERSION.SDK_INT > 17) {
                                of.set(pf);
                            }
                            XposedHelpers.callMethod(param.thisObject, "applyStableConstraints",
                                    sysUiFl, fl, cf);
                            vf.set(cf);
                            if (Build.VERSION.SDK_INT > 17) {
                                XposedHelpers.callMethod(param.args[0], "computeFrameLw", pf, df, of, cf, vf);
                            } else {
                                XposedHelpers.callMethod(param.args[0], "computeFrameLw", pf, df, cf, vf);
                            }
                            if (DEBUG) log("layoutWindowLw recomputing frame");
                        }
                    }
                }
            });

            if (Build.VERSION.SDK_INT > 17) {
                XposedHelpers.findAndHookMethod(windowStateClass, "computeFrameLw",
                        Rect.class, Rect.class, Rect.class, Rect.class, Rect.class, windowStateComputeFrameLw);
            } else {
                XposedHelpers.findAndHookMethod(windowStateClass, "computeFrameLw",
                        Rect.class, Rect.class, Rect.class, Rect.class, windowStateComputeFrameLw);
            }

            XposedHelpers.findAndHookMethod(windowManagerServiceClass, "adjustWallpaperWindowsLocked",
                    adjustWallpaperHook);

            XposedHelpers.findAndHookMethod(windowManagerServiceClass, "updateWallpaperOffsetLocked",
                    CLASS_WINDOW_STATE, boolean.class, adjustWallpaperHook);

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook windowStateComputeFrameLw = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (isNavbarTransparencyEnabled() && 
                    XposedHelpers.getBooleanField(param.thisObject, "mIsWallpaper")) {
                try {
                    final int width = getWallpaperWidth();
                    final int height = getWallpaperHeight();
                    if (width > 0 && height > 0) {
                        XposedHelpers.callMethod(
                                XposedHelpers.getObjectField(param.thisObject, "mService"),
                                "updateWallpaperOffsetLocked",
                                param.thisObject, width, height, false);
                        if (DEBUG) log("updateWallpaperOffsetLocked: width=" + width +
                                "; height=" + height);
                    }
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        }
    };

    private static XC_MethodHook adjustWallpaperHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
            if (!isNavbarTransparencyEnabled()) return;

            try {
                if (Build.VERSION.SDK_INT > 16) {
                    final Class<?> displayContentClass = 
                            XposedHelpers.findClass("com.android.server.wm.DisplayContent", null);
                    if (DEBUG) log ("adjustWallpaperWindowsLocked: hooking getDisplayInfo");
                    mDisplayContentHook = XposedHelpers.findAndHookMethod(displayContentClass, "getDisplayInfo",
                            new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param2) throws Throwable {
                            Object di = XposedHelpers.getObjectField(param2.thisObject, "mDisplayInfo");
                            final int width = getWallpaperWidth();
                            final int height = getWallpaperHeight();
                            if (width > 0 && height > 0) {
                                XposedHelpers.setIntField(di, "appWidth", width);
                                XposedHelpers.setIntField(di, "appHeight", height);
                                param.setResult(di);
                                if (DEBUG) log("adjustWallpaperWindowsLocked: getDisplayInfo appWidth=" + width 
                                        + "; appHeight=" + height);
                            }
                        }
                    });
                } else {
                    final int width = getWallpaperWidth();
                    final int height = getWallpaperHeight();
                    if (width > 0 && height > 0) {
                        int appWidth = XposedHelpers.getIntField(param.thisObject, "mAppDisplayWidth");
                        int appHeight = XposedHelpers.getIntField(param.thisObject, "mAppDisplayHeight");
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mAppDisplayWidthOrig", appWidth);
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mAppDisplayHeightOrig", appHeight);
                        XposedHelpers.setIntField(param.thisObject, "mAppDisplayWidth", width);
                        XposedHelpers.setIntField(param.thisObject, "mAppDisplayHeight", height);
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (!isNavbarTransparencyEnabled()) return;

            try {
                if (Build.VERSION.SDK_INT > 16) {
                    if (mDisplayContentHook != null) {
                        mDisplayContentHook.unhook();
                        mDisplayContentHook = null;
                        if (DEBUG) log ("adjustWallpaperWindowsLocked: unhooking getDisplayInfo");
                    }
                } else {
                    Integer appWidth = (Integer) XposedHelpers.getAdditionalInstanceField(
                            param.thisObject, "mAppDisplayWidthOrig");
                    Integer appHeight = (Integer) XposedHelpers.getAdditionalInstanceField(
                            param.thisObject, "mAppDisplayHeightOrig");
                    if (appWidth != null && appHeight != null) {
                        XposedHelpers.setIntField(param.thisObject, "mAppDisplayWidth", (int)appWidth);
                        XposedHelpers.setIntField(param.thisObject, "mAppDisplayHeight", (int)appHeight);
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static boolean isTransparencyEnabled() {
        boolean enabled = false;
        for (int i = 0; i < 4; i++) {
            enabled |= mTransparencyValuesPwm[i] != 0;
        }
        return enabled;
    }

    private static boolean isNavbarTransparencyEnabled() {
        return (mTransparencyValuesPwm[2] != 0 ||
                mTransparencyValuesPwm[3] != 0);
    }

    private static int getWallpaperWidth() {
        if (mPhoneWindowManager != null) {
            final int left = XposedHelpers.getIntField(mPhoneWindowManager, "mUnrestrictedScreenLeft");
            final int width = XposedHelpers.getIntField(mPhoneWindowManager, "mUnrestrictedScreenWidth");
            return left + (left + width);
        }
        return 0;
    }

    private static int getWallpaperHeight() {
        if (mPhoneWindowManager != null) {
            final int top = XposedHelpers.getIntField(mPhoneWindowManager, "mUnrestrictedScreenTop");
            final int height = XposedHelpers.getIntField(mPhoneWindowManager, "mUnrestrictedScreenHeight");
            return top + (top + height);
        }
        return 0;
    }

    // in process hooks
    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> phoneStatusbarViewClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR_VIEW, classLoader);
            final Class<?> phoneStatusbarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> signalClusterViewClass = XposedHelpers.findClass(CLASS_SIGNAL_CLUSTER_VIEW, classLoader);
            final Class<?> batteryControllerClass = XposedHelpers.findClass(CLASS_BATTERY_CONTROLLER, classLoader);
            final Class<?> notifPanelViewClass = Build.VERSION.SDK_INT > 16 ?
                    XposedHelpers.findClass(CLASS_NOTIF_PANEL_VIEW, classLoader) : null;
            final Class<?> statusbarIconViewClass = XposedHelpers.findClass(CLASS_STATUSBAR_ICON_VIEW, classLoader);

            mBroadcastSubReceivers = new ArrayList<BroadcastSubReceiver>();

            mIconColorEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE, false);
            mSkipBatteryIcon = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_COLOR_SKIP_BATTERY, false);

            mRoamingIndicatorsDisabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_DISABLE_ROAMING_INDICATORS, false);

            XposedBridge.hookAllConstructors(phoneStatusbarViewClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPanelBar = (View) param.thisObject;

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_NOTIF_BACKGROUND_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_DISABLE_ROAMING_INDICATORS_CHANGED);
                    mPanelBar.getContext().registerReceiver(mBroadcastReceiver, intentFilter);

                    Context gbContext = mPanelBar.getContext().createPackageContext(GravityBox.PACKAGE_NAME,
                            Context.CONTEXT_IGNORE_SECURITY);
                    mIconManager = new StatusBarIconManager(gbContext.getResources(), 
                            mPanelBar.getContext().getResources());
                    mIconManager.setIconColor(
                            prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_ICON_COLOR,
                                    mIconManager.getDefaultIconColor()));
                    mIconManager.setDataActivityColor(
                            prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR, 
                                    StatusBarIconManager.DEFAULT_DATA_ACTIVITY_COLOR));
                    mIconManager.setFollowStockBatteryColor(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_STATUSBAR_COLOR_FOLLOW_STOCK_BATTERY, false));

                    try {
                        int signalIconMode = Integer.valueOf(prefs.getString(
                                GravityBoxSettings.PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE, "0"));
                        mIconManager.setSignalIconMode(signalIconMode);
                    } catch (NumberFormatException nfe) {
                        log("Invalid value for PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE preference");
                    }
                    mIconManager.initStockBatteryColor(mPanelBar.getContext());
                }
            });

            XposedBridge.hookAllConstructors(signalClusterViewClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mSignalClusterView = param.thisObject;
                    if (DEBUG) log("SignalClusterView constructed - mSignalClusterView set");
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, 
                    "makeStatusBarView", new XC_MethodHook(XCallback.PRIORITY_LOWEST) {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPhoneStatusBar = param.thisObject;
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                    mTransparencyManager = new TransparencyManager(context);
                    mTransparencyManager.setStatusbar(XposedHelpers.getObjectField(param.thisObject, "mStatusBarView"));
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_OVERRIDE, false)) {
                        mTransparencyManager.setNavbar(XposedHelpers.getObjectField(
                                param.thisObject, "mNavigationBarView"));
                    }
                    mTransparencyManager.initPreferences(prefs);
                    mBroadcastSubReceivers.add(mTransparencyManager);

                    mBatteryController = XposedHelpers.getObjectField(param.thisObject, "mBatteryController");
                    prefs.reload();
                    int bgColor = prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_BGCOLOR, Color.BLACK);
                    setStatusbarBgColor(bgColor);
                    applyIconColors();

                    Intent i = new Intent(ACTION_PHONE_STATUSBAR_VIEW_MADE);
                    context.sendBroadcast(i);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "getNavigationBarLayoutParams", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.getResult();
                    if (lp != null) {
                        lp.format = PixelFormat.TRANSLUCENT;
                        param.setResult(lp);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "disable", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mTransparencyManager != null) {
                        mTransparencyManager.update();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "topAppWindowChanged",
                    boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mTransparencyManager != null) {
                        mTransparencyManager.update();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "setStatusBarLowProfile",
                    boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final boolean lightsOut = (Boolean) param.args[0];
                    if (mCircleBattery != null) {
                        mCircleBattery.setLowProfile(lightsOut);
                    }
                    if (mPercentage != null) {
                        mPercentage.setAlpha(lightsOut ? 0.5f : 1);
                    }
                    if (mTrafficMeter != null) {
                        mTrafficMeter.setAlpha(lightsOut ? 0 : 1);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(batteryControllerClass, "onReceive",
                    Context.class, Intent.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[1];
                    if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                        mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                        if (Build.VERSION.SDK_INT > 17) {
                            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 
                                    BatteryManager.BATTERY_STATUS_UNKNOWN);
                            mBatteryPlugged = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                    status == BatteryManager.BATTERY_STATUS_FULL);
                        } else {
                            mBatteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
                        }
                        if (mIconColorEnabled && !mSkipBatteryIcon && mBattery != null
                                && mIconManager != null) {
                            Drawable d = mIconManager.getBatteryIcon(mBatteryLevel, mBatteryPlugged);
                            if (d != null) mBattery.setImageDrawable(d);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(signalClusterViewClass, "apply", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (XposedHelpers.getObjectField(param.thisObject, "mWifiGroup") == null) return;

                    Resources res = ((LinearLayout) param.thisObject).getContext().getResources();

                    if (mIconColorEnabled && mIconManager != null) {
                        Object mobileIconId = null;
                        Object[] mobileIconIds = null, mobileIconIdsGemini = null;
                        Object mobileActivityId = null, mobileActivityIdGemini = null;
                        Object mobileTypeId = null, mobileTypeIdGemini = null;
                        if (Utils.isMtkDevice()) {
                            if (Utils.hasGeminiSupport()) {
                                mobileIconIds = (Object[]) XposedHelpers.getObjectField(param.thisObject, "mMobileStrengthId");
                                mobileIconIdsGemini = (Object[]) XposedHelpers.getObjectField(param.thisObject, "mMobileStrengthIdGemini");
                                mobileActivityIdGemini = XposedHelpers.getObjectField(param.thisObject, "mMobileActivityIdGemini");
                                mobileTypeIdGemini = XposedHelpers.getObjectField(param.thisObject, "mMobileTypeIdGemini");
                            } else {
                                mobileIconId = (Object) XposedHelpers.getObjectField(param.thisObject, "mMobileStrengthId");
                            }
                            mobileActivityId = XposedHelpers.getObjectField(param.thisObject, "mMobileActivityId");
                            mobileTypeId = XposedHelpers.getObjectField(param.thisObject, "mMobileTypeId");
                        }

                        if (XposedHelpers.getBooleanField(param.thisObject, "mWifiVisible") &&
                                mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                            ImageView wifiIcon = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mWifi");
                            if (wifiIcon != null) {
                                int resId = XposedHelpers.getIntField(param.thisObject, "mWifiStrengthId");
                                Drawable d = mIconManager.getWifiIcon(resId);
                                if (d != null) wifiIcon.setImageDrawable(d);
                            }
                            ImageView wifiActivity = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mWifiActivity");
                            if (wifiActivity != null) {
                                try {
                                    int resId = XposedHelpers.getIntField(param.thisObject, "mWifiActivityId");
                                    Drawable d = res.getDrawable(resId).mutate();
                                    d = mIconManager.applyDataActivityColorFilter(d);
                                    wifiActivity.setImageDrawable(d);
                                } catch (Resources.NotFoundException e) {
                                    wifiActivity.setImageDrawable(null);
                                }
                            }
                        }
    
                        if (!XposedHelpers.getBooleanField(param.thisObject, "mIsAirplaneMode")) {
                            // for SIM Slot 1
                            if (XposedHelpers.getBooleanField(param.thisObject, "mMobileVisible") &&
                                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                                ImageView mobile = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobile");
                                if (mobile != null) {
                                    int resId = Utils.isMtkDevice() ? 
                                            (Integer) XposedHelpers.callMethod(Utils.hasGeminiSupport() ?
                                            		mobileIconIds[0] : mobileIconId, "getIconId") :
                                            XposedHelpers.getIntField(param.thisObject, "mMobileStrengthId");
                                    Drawable d = mIconManager.getMobileIcon(resId);
                                    if (d != null) mobile.setImageDrawable(d);
                                }
                                if (mIconManager.isMobileIconChangeAllowed()) {
                                    ImageView mobileActivity = 
                                            (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileActivity");
                                    if (mobileActivity != null) {
                                        try {
                                            int resId = Utils.isMtkDevice() ? 
                                                    (Integer) XposedHelpers.callMethod(mobileActivityId, "getIconId") :
                                                    XposedHelpers.getIntField(param.thisObject, "mMobileActivityId");
                                            Drawable d = res.getDrawable(resId).mutate();
                                            d = mIconManager.applyDataActivityColorFilter(d);
                                            mobileActivity.setImageDrawable(d);
                                        } catch (Resources.NotFoundException e) { 
                                            mobileActivity.setImageDrawable(null);
                                        }
                                    }
                                    ImageView mobileType = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileType");
                                    if (mobileType != null) {
                                        try {
                                            int resId = Utils.hasGeminiSupport() ?
                                                    (Integer) XposedHelpers.callMethod(mobileTypeId, "getIconId") :
                                                    XposedHelpers.getIntField(param.thisObject, "mMobileTypeId");
                                            Drawable d = res.getDrawable(resId).mutate();
                                            d = mIconManager.applyColorFilter(d);
                                            mobileType.setImageDrawable(d);
                                        } catch (Resources.NotFoundException e) { 
                                            mobileType.setImageDrawable(null);
                                        }
                                    }
                                    if (Utils.isMtkDevice() &&
                                            XposedHelpers.getBooleanField(param.thisObject, "mRoaming")) {
                                        ImageView mobileRoam = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileRoam");
                                        if (mobileRoam != null) {
                                            try {
                                                int resId = XposedHelpers.getIntField(param.thisObject, "mRoamingId");
                                                Drawable d = res.getDrawable(resId).mutate();
                                                d = mIconManager.applyColorFilter(d);
                                                mobileRoam.setImageDrawable(d);
                                            } catch (Resources.NotFoundException e) { 
                                                mobileRoam.setImageDrawable(null);
                                            }
                                        }
                                    }
                                }
                            }
    
                            // for SIM Slot 2
                            if (Utils.hasGeminiSupport() && 
                                    XposedHelpers.getBooleanField(param.thisObject, "mMobileVisibleGemini") &&
                                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                                ImageView mobile = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileGemini");
                                if (mobile != null) {
                                    int resId = (Integer) XposedHelpers.callMethod(mobileIconIdsGemini[0], "getIconId");
                                    Drawable d = mIconManager.getMobileIcon(resId);
                                    if (d != null) mobile.setImageDrawable(d);
                                }
                                if (mIconManager.isMobileIconChangeAllowed()) {
                                    ImageView mobileActivity = 
                                            (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileActivityGemini");
                                    if (mobileActivity != null) {
                                        try {
                                            int resId = (Integer) XposedHelpers.callMethod(mobileActivityIdGemini, "getIconId");
                                            Drawable d = res.getDrawable(resId).mutate();
                                            d = mIconManager.applyDataActivityColorFilter(d);
                                            mobileActivity.setImageDrawable(d);
                                        } catch (Resources.NotFoundException e) { 
                                            mobileActivity.setImageDrawable(null);
                                        }
                                    }
                                    ImageView mobileType = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileTypeGemini");
                                    if (mobileType != null) {
                                        try {
                                            int resId = (Integer) XposedHelpers.callMethod(mobileTypeIdGemini, "getIconId");
                                            Drawable d = res.getDrawable(resId).mutate();
                                            d = mIconManager.applyColorFilter(d);
                                            mobileType.setImageDrawable(d);
                                        } catch (Resources.NotFoundException e) { 
                                            mobileType.setImageDrawable(null);
                                        }
                                    }
                                    if (XposedHelpers.getBooleanField(param.thisObject, "mRoamingGemini")) {
                                        ImageView mobileRoam = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileRoamGemini");
                                        if (mobileRoam != null) {
                                            try {
                                                int resId = XposedHelpers.getIntField(param.thisObject, "mRoamingGeminiId");
                                                Drawable d = res.getDrawable(resId).mutate();
                                                d = mIconManager.applyColorFilter(d);
                                                mobileRoam.setImageDrawable(d);
                                            } catch (Resources.NotFoundException e) { 
                                                mobileRoam.setImageDrawable(null);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (Utils.isMtkDevice() && mRoamingIndicatorsDisabled) {
                        ImageView mobileRoam;
                        mobileRoam = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileRoam");
                        if (mobileRoam != null) mobileRoam.setVisibility(View.GONE);
                        if (Utils.hasGeminiSupport()) {
                            mobileRoam = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileRoamGemini");
                            if (mobileRoam != null) mobileRoam.setVisibility(View.GONE);
                        }
                    }
                }
            });

            if (notifPanelViewClass != null) {
                XposedHelpers.findAndHookMethod(notifPanelViewClass, "onFinishInflate", new XC_MethodHook() {
    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mNotificationPanelView = (FrameLayout) param.thisObject;
    
                        mNotificationWallpaper = new NotificationWallpaper(mNotificationPanelView.getContext());
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT);
                        mNotificationWallpaper.setLayoutParams(lp);
                        mNotificationWallpaper.setType(prefs.getString(
                                GravityBoxSettings.PREF_KEY_NOTIF_BACKGROUND,
                                GravityBoxSettings.NOTIF_BG_DEFAULT));
                        mNotificationWallpaper.setColor(prefs.getInt(
                                GravityBoxSettings.PREF_KEY_NOTIF_COLOR, Color.BLACK));
                        mNotificationWallpaper.setColorMode(prefs.getString(
                                GravityBoxSettings.PREF_KEY_NOTIF_COLOR_MODE,
                                GravityBoxSettings.NOTIF_BG_COLOR_MODE_OVERLAY));
                        mNotificationWallpaper.setAlpha(prefs.getInt(
                                GravityBoxSettings.PREF_KEY_NOTIF_BACKGROUND_ALPHA, 60));
                        mNotificationPanelView.addView(mNotificationWallpaper);
                        updateNotificationPanelBackground();
                    }
                });
            }

            XposedHelpers.findAndHookMethod(statusbarIconViewClass, "getIcon",
                    CLASS_STATUSBAR_ICON, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mIconColorEnabled && mIconManager != null) {
                        final int iconId = XposedHelpers.getIntField(param.args[0], "iconId");
                        Drawable d = mIconManager.getBasicIcon(iconId);
                        if (d != null) {
                            ((ImageView)param.thisObject).setTag("GBColoredView");
                            param.setResult(d);
                            return;
                        }
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setStatusbarBgColor(int color) {
        if (mPanelBar != null) {
            if (Utils.isXperiaDevice()) {
                if (!(mPanelBar.getBackground() instanceof ColorDrawable)) {
                    ColorDrawable colorDrawable = new ColorDrawable(color);
                    mPanelBar.setBackground(colorDrawable);
                    if (DEBUG) log("statusbar view backround replaced with ColorDrawable");
                } else {
                    ((ColorDrawable) mPanelBar.getBackground()).setColor(color);
                }
            } else {
                if (!(mPanelBar.getBackground() instanceof BackgroundAlphaColorDrawable)) {
                    BackgroundAlphaColorDrawable colorDrawable = new BackgroundAlphaColorDrawable(color);
                    mPanelBar.setBackground(colorDrawable);
                    if (DEBUG) log("statusbar view backround replaced with BackgroundAlphaColorDrawable");
                } else {
                    ((BackgroundAlphaColorDrawable) mPanelBar.getBackground()).setBgColor(color);
                }
            }
            if (DEBUG) log("statusbar background color set to: " + color);
        }
    }

    private static void applyIconColors() {
        if (mIconManager == null) return;

        try {
            if (mSignalClusterView != null) {
                XposedHelpers.callMethod(mSignalClusterView, "apply");
            }
    
            if (mClock != null) {
                if (mIconManager.getDefaultClockColor() == null) {
                    mIconManager.setDefaultClockColor(mClock.getCurrentTextColor());
                }
                mClock.setTextColor(mIconColorEnabled ? 
                        mIconManager.getIconColor() : mIconManager.getClockColor());
            }
    
            if (mCircleBattery != null) {
                mCircleBattery.setColor(mIconColorEnabled ?
                        mIconManager.getIconColor() : mIconManager.getDefaultIconColor());
            }
    
            if (mTrafficMeter != null) {
                mTrafficMeter.setTextColor(mIconColorEnabled ?
                        mIconManager.getIconColor() : mIconManager.getDefaultIconColor());
            }
    
            if (mPercentage != null) {
                if (mIconManager.getDefaultBatteryPercentageColor() == null) {
                    mIconManager.setDefaultBatteryPercentageColor(mPercentage.getCurrentTextColor());
                }
                mPercentage.setTextColor(mIconColorEnabled ? 
                        mIconManager.getIconColor() : mIconManager.getBatteryPercentageColor());
            }
    
            if (mBatteryController != null && mBattery != null) {
                Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
                intent.putExtra(BatteryManager.EXTRA_LEVEL, mBatteryLevel);
                if (Build.VERSION.SDK_INT > 17) {
                    intent.putExtra(BatteryManager.EXTRA_STATUS, mBatteryPlugged ? 
                            BatteryManager.BATTERY_STATUS_CHARGING :
                                BatteryManager.BATTERY_STATUS_UNKNOWN);
                } else {
                    intent.putExtra(BatteryManager.EXTRA_PLUGGED, mBatteryPlugged ? 1 : 0);
                }
                try {
                    XposedHelpers.callMethod(mBatteryController, "onReceive", mBattery.getContext(), intent);
                } catch (Throwable t) {
                    log("Incompatible battery controller: " + t.getMessage());
                }
            }
    
            if (mPhoneStatusBar != null) {
                ViewGroup vg = (ViewGroup) XposedHelpers.getObjectField(mPhoneStatusBar, "mStatusIcons");
                final int childCount = vg.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    if (!vg.getChildAt(i).getClass().getName().equals(CLASS_STATUSBAR_ICON_VIEW)) {
                        continue;
                    }
                    ImageView v = (ImageView) vg.getChildAt(i);
                    if (!mIconColorEnabled && "GBColoredView".equals(v.getTag())) {
                        Drawable d = v.getDrawable();
                        if (d != null) {
                            d.setColorFilter(null);
                        }
                    } else if (mIconColorEnabled) {
                        final Object sbIcon = XposedHelpers.getObjectField(v, "mIcon");
                        if (sbIcon != null) {
                            final int resId = XposedHelpers.getIntField(sbIcon, "iconId");
                            Drawable d = mIconManager.getBasicIcon(resId);
                            if (d != null) {
                                v.setImageDrawable(d);
                                v.setTag("GBColoredView");
                            }
                        }
                    }
                }
            }

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void updateNotificationPanelBackground() {
        if (mNotificationPanelView == null || mNotificationWallpaper == null) return;

        mNotificationPanelView.setBackgroundResource(0);
        mNotificationPanelView.setBackgroundResource(
                mNotificationPanelView.getResources().getIdentifier(
                        "notification_panel_bg", "drawable", PACKAGE_NAME));
        Drawable background = mNotificationPanelView.getBackground();
        float alpha = mNotificationWallpaper.getAlpha();
        background.setAlpha(alpha == 0 ? 255 : 
            (int)(1-alpha * 255));

        mNotificationWallpaper.updateNotificationWallpaper();
    }
}