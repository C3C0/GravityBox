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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModExpandedDesktop {
    private static final String TAG = "GB:ModExpandedDesktop";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_POLICY_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";
    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
    private static final String CLASS_LOCAL_POWER_MANAGER = "android.os.LocalPowerManager";

    private static final boolean DEBUG = false;

    private static Context mContext;
    private static Object mPhoneWindowManager;
    private static SettingsObserver mSettingsObserver;
    private static boolean mExpandedDesktop;
    private static int mExpandedDesktopMode;
    private static Unhook mNavbarShowLwHook;
    private static Unhook mStatusbarShowLwHook;
    private static boolean mNavbarOverride;
    private static float mNavbarHeightScaleFactor = 1;
    private static float mNavbarHeightLandscapeScaleFactor = 1;
    private static float mNavbarWidthScaleFactor = 1;

    public static final String SETTING_EXPANDED_DESKTOP_STATE = "gravitybox_expanded_desktop_state";
    private static final int SEND_NEW_CONFIGURATION = 18;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    static class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SETTING_EXPANDED_DESKTOP_STATE), false, this);
            updateSettings();
        }

        @Override 
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED)
                    && intent.hasExtra(GravityBoxSettings.EXTRA_ED_MODE)) {
                final int expandedDesktopMode = intent.getIntExtra(
                        GravityBoxSettings.EXTRA_ED_MODE, GravityBoxSettings.ED_DISABLED);
                final boolean forceUpdateDisplayMetrics = expandedDesktopMode != mExpandedDesktopMode;
                mExpandedDesktopMode = expandedDesktopMode;
                updateSettings(forceUpdateDisplayMetrics);
            } else if (intent.getAction().equals(ModStatusbarColor.ACTION_PHONE_STATUSBAR_VIEW_MADE)) {
                updateSettings(true);
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_HEIGHT)) {
                    mNavbarHeightScaleFactor = 
                            (float)intent.getIntExtra(GravityBoxSettings.EXTRA_NAVBAR_HEIGHT, 100) / 100f;
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_HEIGHT_LANDSCAPE)) {
                    mNavbarHeightLandscapeScaleFactor = (float)intent.getIntExtra(
                                    GravityBoxSettings.EXTRA_NAVBAR_HEIGHT_LANDSCAPE,  100) / 100f;
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_WIDTH)) {
                    mNavbarWidthScaleFactor = 
                            (float)intent.getIntExtra(GravityBoxSettings.EXTRA_NAVBAR_WIDTH, 100) / 100f;
                }
                updateSettings(true);
            }
        }
    };

    private static void updateSettings() {
        updateSettings(false);
    }

    private static void updateSettings(boolean forceUpdateDisplayMetrics) {
        if (mContext == null || mPhoneWindowManager == null) return;

        try {
            final boolean expandedDesktop = Settings.System.getInt(mContext.getContentResolver(), 
                    SETTING_EXPANDED_DESKTOP_STATE, 0) == 1;
            if (mExpandedDesktopMode == GravityBoxSettings.ED_DISABLED && expandedDesktop) {
                    Settings.System.putInt(mContext.getContentResolver(),
                            SETTING_EXPANDED_DESKTOP_STATE, 0);
                    return;
            }

            boolean updateDisplayMetrics = false | forceUpdateDisplayMetrics;
            if (mExpandedDesktop != expandedDesktop) {
                mExpandedDesktop = expandedDesktop;
                updateDisplayMetrics = true;
            }

            XposedHelpers.callMethod(mPhoneWindowManager, "updateSettings");

            int[] navigationBarWidthForRotation = (int[]) XposedHelpers.getObjectField(
                    mPhoneWindowManager, "mNavigationBarWidthForRotation");
            int[] navigationBarHeightForRotation = (int[]) XposedHelpers.getObjectField(
                    mPhoneWindowManager, "mNavigationBarHeightForRotation");
            final int portraitRotation = XposedHelpers.getIntField(mPhoneWindowManager, "mPortraitRotation");
            final int upsideDownRotation = XposedHelpers.getIntField(mPhoneWindowManager, "mUpsideDownRotation");
            final int landscapeRotation = XposedHelpers.getIntField(mPhoneWindowManager, "mLandscapeRotation");
            final int seascapeRotation = XposedHelpers.getIntField(mPhoneWindowManager, "mSeascapeRotation");

            if (expandedDesktopHidesNavigationBar()) {
                navigationBarWidthForRotation[portraitRotation]
                        = navigationBarWidthForRotation[upsideDownRotation]
                        = navigationBarWidthForRotation[landscapeRotation]
                        = navigationBarWidthForRotation[seascapeRotation]
                        = navigationBarHeightForRotation[portraitRotation]
                        = navigationBarHeightForRotation[upsideDownRotation]
                        = navigationBarHeightForRotation[landscapeRotation]
                        = navigationBarHeightForRotation[seascapeRotation] = 0;
            } else {
                final int resWidthId = mContext.getResources().getIdentifier(
                        "navigation_bar_width", "dimen", "android");
                final int resHeightId = mContext.getResources().getIdentifier(
                        "navigation_bar_height", "dimen", "android");
                final int resHeightLandscapeId = mContext.getResources().getIdentifier(
                        "navigation_bar_height_landscape", "dimen", "android");

                navigationBarHeightForRotation[portraitRotation] =
                navigationBarHeightForRotation[upsideDownRotation] =
                    (int) (mContext.getResources().getDimensionPixelSize(resHeightId)
                    * mNavbarHeightScaleFactor);
                navigationBarHeightForRotation[landscapeRotation] =
                navigationBarHeightForRotation[seascapeRotation] =
                    (int) (mContext.getResources().getDimensionPixelSize(resHeightLandscapeId)
                    * mNavbarHeightLandscapeScaleFactor);

                navigationBarWidthForRotation[portraitRotation] =
                navigationBarWidthForRotation[upsideDownRotation] =
                navigationBarWidthForRotation[landscapeRotation] =
                navigationBarWidthForRotation[seascapeRotation] =
                    (int) (mContext.getResources().getDimensionPixelSize(resWidthId)
                    * mNavbarWidthScaleFactor);
            }

            XposedHelpers.setObjectField(mPhoneWindowManager, "mNavigationBarWidthForRotation", navigationBarWidthForRotation);
            XposedHelpers.setObjectField(mPhoneWindowManager, "mNavigationBarHeightForRotation", navigationBarHeightForRotation);

            XposedHelpers.callMethod(mPhoneWindowManager, "updateRotation", false);
            if (updateDisplayMetrics) {
                updateDisplayMetrics(XposedHelpers.getObjectField(mPhoneWindowManager, "mWindowManager"));
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> classPhoneWindowManager = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);

            mNavbarOverride = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_OVERRIDE, false);
            if (mNavbarOverride) {
                mNavbarHeightScaleFactor = 
                        (float) prefs.getInt(GravityBoxSettings.PREF_KEY_NAVBAR_HEIGHT, 100) / 100f;
                mNavbarHeightLandscapeScaleFactor = 
                        (float) prefs.getInt(GravityBoxSettings.PREF_KEY_NAVBAR_HEIGHT_LANDSCAPE, 100) / 100f;
                mNavbarWidthScaleFactor = 
                        (float) prefs.getInt(GravityBoxSettings.PREF_KEY_NAVBAR_WIDTH, 100) / 100f;
            }

            mExpandedDesktopMode = GravityBoxSettings.ED_DISABLED;
            try {
                mExpandedDesktopMode = Integer.valueOf(prefs.getString(
                        GravityBoxSettings.PREF_KEY_EXPANDED_DESKTOP, "0"));
            } catch (NumberFormatException nfe) {
                log("Invalid value for PREF_KEY_EXPANDED_DESKTOP preference");
            }

            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init",
                    Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, phoneWindowManagerInitHook);
            } else {
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init",
                        Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, 
                        CLASS_LOCAL_POWER_MANAGER, phoneWindowManagerInitHook);
            }

            XposedHelpers.findAndHookMethod(classPhoneWindowManager,
                    Build.VERSION.SDK_INT > 16 ? 
                            "finishPostLayoutPolicyLw" : "finishAnimationLw", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    final Object statusBar = XposedHelpers.getObjectField(param.thisObject, "mStatusBar");
                    if (statusBar == null || !expandedDesktopHidesStatusbar()) return;

                    mStatusbarShowLwHook = XposedHelpers.findAndHookMethod(
                            statusBar.getClass(), "showLw", boolean.class, new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param2) throws Throwable {
                                    if (param2.thisObject == statusBar) {
                                        return XposedHelpers.callMethod(param2.thisObject, "hideLw", true);
                                    } else {
                                        return XposedBridge.invokeOriginalMethod(param2.method, param2.thisObject, param2.args);
                                    }
                                }
                    });
                }
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mStatusbarShowLwHook != null) {
                        mStatusbarShowLwHook.unhook();
                        mStatusbarShowLwHook = null;
                    }
                }
            });

            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "beginLayoutLw",
                        boolean.class, int.class, int.class, int.class, beginLayoutLwHook);
            } else {
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "beginLayoutLw",
                        int.class, int.class, int.class, beginLayoutLwHook);
            }

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "layoutWindowLw",
                    CLASS_POLICY_WINDOW_STATE, WindowManager.LayoutParams.class, 
                    CLASS_POLICY_WINDOW_STATE, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final boolean isDefaultDisplay = Build.VERSION.SDK_INT > 16 ?
                            (Boolean) XposedHelpers.callMethod(param.args[0], "isDefaultDisplay") : true;
                    if (!mExpandedDesktop
                            || param.args[0] == XposedHelpers.getObjectField(param.thisObject, "mStatusBar")
                            || param.args[0] == XposedHelpers.getObjectField(param.thisObject, "mNavigationBar")
                            || !isDefaultDisplay) return;

                    final WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) param.args[1];
                    if (attrs.type == WindowManager.LayoutParams.TYPE_INPUT_METHOD) return;

                    final int fl = ((WindowManager.LayoutParams) param.args[1]).flags;
                    final int sysUiFl = (Integer) XposedHelpers.callMethod(param.args[0], "getSystemUiVisibility");
                    final Rect pf = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpParentFrame");
                    final Rect df = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpDisplayFrame");
                    final Rect cf = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpContentFrame");
                    final Rect vf = (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpVisibleFrame");
                    final Rect of = Build.VERSION.SDK_INT > 17 ?
                            (Rect) XposedHelpers.getObjectField(param.thisObject, "mTmpOverscanFrame") : null;

                    boolean shouldRecomputeFrame = false;
                    if ((fl & (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | 
                               WindowManager.LayoutParams.FLAG_FULLSCREEN | 
                               WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR))
                            == (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | 
                                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR)
                            && (sysUiFl & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        if (param.args[2] == null
                                && attrs.type >= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW
                                && attrs.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
                            pf.left = df.left = Build.VERSION.SDK_INT > 17 ?
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenLeft") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenLeft");
                            pf.right = df.right = pf.left + Build.VERSION.SDK_INT > 17 ?
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenWidth") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenWidth");
                            pf.top = df.top = Build.VERSION.SDK_INT > 17 ?
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenTop") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenTop");
                            pf.bottom = df.bottom = pf.top + Build.VERSION.SDK_INT > 17 ?
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenHeight") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenHeight");
                            if (Build.VERSION.SDK_INT > 17) {
                                of.left = XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenLeft");
                                of.top = XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenTop");
                                of.right = of.left + 
                                        XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenWidth");
                                of.bottom = of.top + 
                                        XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenHeight");
                            }
                            if (expandedDesktopHidesStatusbar()) {
                                cf.top = pf.top;
                            }
                            shouldRecomputeFrame = true;
                        }
                    } else if ((fl & WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN) != 0 || (sysUiFl
                                & (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                           | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)) != 0) {
                        if (attrs.type >= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW
                                && attrs.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
                            pf.left = df.left = Build.VERSION.SDK_INT > 17 ?
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenLeft") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenLeft");
                            pf.right = df.right = pf.left + Build.VERSION.SDK_INT > 17 ?
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenWidth") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenWidth");
                            pf.top = df.top = Build.VERSION.SDK_INT > 17 ?
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenTop") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenTop");
                            pf.bottom = df.bottom = pf.top + Build.VERSION.SDK_INT > 17 ? 
                                    XposedHelpers.getIntField(param.thisObject, "mOverscanScreenHeight") :
                                    XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenHeight");
                            if (Build.VERSION.SDK_INT > 17) {
                                of.set(pf);
                            }
                            if (expandedDesktopHidesNavigationBar()) {
                                cf.set(pf);
                            }
                            shouldRecomputeFrame = true;
                        }
                    }

                    if (shouldRecomputeFrame) {
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
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "getContentInsetHintLw",
                    WindowManager.LayoutParams.class, Rect.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    if (!mExpandedDesktop) {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        return null;
                    }

                    try {
                        final WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) param.args[0];
                        final Rect contentInset = (Rect) param.args[1];
                        final int fl = attrs.flags;
                        final int systemUiVisibility = (attrs.systemUiVisibility |
                                XposedHelpers.getIntField(attrs, "subtreeSystemUiVisibility"));

                        if ((fl & (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | 
                                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR))
                                == (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | 
                                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR)) {
                            int availRight, availBottom;
                            if (shouldHideNavigationBarLw(systemUiVisibility)) {
                                availRight = XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenLeft") 
                                        + XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenWidth");
                                availBottom = XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenTop")
                                        + XposedHelpers.getIntField(param.thisObject, "mUnrestrictedScreenHeight");
                            } else {
                                availRight = XposedHelpers.getIntField(param.thisObject, "mRestrictedScreenLeft")
                                        + XposedHelpers.getIntField(param.thisObject, "mRestrictedScreenWidth");
                                availBottom = XposedHelpers.getIntField(param.thisObject, "mRestrictedScreenTop")
                                        + XposedHelpers.getIntField(param.thisObject, "mRestrictedScreenHeight");
                            }
                            if ((systemUiVisibility & View.SYSTEM_UI_FLAG_LAYOUT_STABLE) != 0) {
                                if ((fl & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
                                    contentInset.set(XposedHelpers.getIntField(param.thisObject, "mStableFullscreenLeft"),
                                            XposedHelpers.getIntField(param.thisObject, "mStableFullscreenTop"),
                                            availRight - XposedHelpers.getIntField(param.thisObject, "mStableFullscreenRight"),
                                            availBottom - XposedHelpers.getIntField(param.thisObject, "mStableFullscreenBottom"));
                                } else {
                                    contentInset.set(XposedHelpers.getIntField(param.thisObject, "mStableLeft"), 
                                            XposedHelpers.getIntField(param.thisObject, "mStableTop"),
                                            availRight - XposedHelpers.getIntField(param.thisObject, "mStableRight"), 
                                            availBottom - XposedHelpers.getIntField(param.thisObject, "mStableBottom"));
                                }
                            } else if ((fl & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
                                contentInset.setEmpty();
                            } else if ((systemUiVisibility & (View.SYSTEM_UI_FLAG_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)) == 0) {
                                contentInset.set(XposedHelpers.getIntField(param.thisObject, "mCurLeft"), 
                                        XposedHelpers.getIntField(param.thisObject, "mCurTop"),
                                        availRight - XposedHelpers.getIntField(param.thisObject, "mCurRight"), 
                                        availBottom - XposedHelpers.getIntField(param.thisObject, "mCurBottom"));
                            } else {
                                contentInset.set(XposedHelpers.getIntField(param.thisObject, "mCurLeft"), 
                                        XposedHelpers.getIntField(param.thisObject, "mCurTop"),
                                        availRight - XposedHelpers.getIntField(param.thisObject, "mCurRight"), 
                                        availBottom - XposedHelpers.getIntField(param.thisObject, "mCurBottom"));
                            }
                            return null;
                        }
                        contentInset.setEmpty();
                    } catch(Throwable t) {
                        if (DEBUG) log(t.getMessage());
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    }

                    return null;
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook phoneWindowManagerInitHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                mPhoneWindowManager = param.thisObject;

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(GravityBoxSettings.ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED);
                intentFilter.addAction(ModStatusbarColor.ACTION_PHONE_STATUSBAR_VIEW_MADE);
                if (mNavbarOverride) {
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED);
                }
                mContext.registerReceiver(mBroadcastReceiver, intentFilter);

                mSettingsObserver = new SettingsObserver(
                        (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler"));
                mSettingsObserver.observe();

                if (DEBUG) log("Phone window manager initialized");
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static XC_MethodHook beginLayoutLwHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
            try {
                final boolean isDefaultDisplay = Build.VERSION.SDK_INT > 16 ?
                        (Boolean) param.args[0] : true;
                if (!isDefaultDisplay || !expandedDesktopHidesNavigationBar()) return;

                final Object navigationBar = XposedHelpers.getObjectField(param.thisObject, "mNavigationBar");
                if (navigationBar == null) return;

                mNavbarShowLwHook = XposedHelpers.findAndHookMethod(navigationBar.getClass(), 
                        "showLw", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final MethodHookParam param2) throws Throwable {
                        if (param2.thisObject == navigationBar) {
                            param2.setResult(false);
                            XposedHelpers.callMethod(navigationBar, "hideLw", true);
                            return;
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            try {
                if (mNavbarShowLwHook != null) {
                    mNavbarShowLwHook.unhook();
                    mNavbarShowLwHook = null;
                }

                final boolean isDefaultDisplay = Build.VERSION.SDK_INT > 16 ?
                        (Boolean) param.args[0] : true;
                        
                if (!isDefaultDisplay) return;

                if (expandedDesktopHidesStatusbar()) {
                    final Object statusBar = XposedHelpers.getObjectField(param.thisObject, "mStatusBar");
                    if (statusBar != null) {
                        XposedHelpers.setIntField(param.thisObject, "mStableTop", 0);
                    }
                }

                if (expandedDesktopHidesNavigationBar() && Build.VERSION.SDK_INT > 16) {
                    final Object navigationBar = XposedHelpers.getObjectField(param.thisObject, "mNavigationBar");
                    if (navigationBar != null) {
                        int overscanLeft = 0;
                        int overscanTop = 0;
                        int overscanRight = 0;
                        int overscanBottom = 0;
                        if (Build.VERSION.SDK_INT > 17) {
                            switch ((Integer) param.args[3]) {
                                case Surface.ROTATION_90:
                                    overscanLeft = XposedHelpers.getIntField(param.thisObject, "mOverscanTop");
                                    overscanTop = XposedHelpers.getIntField(param.thisObject, "mOverscanRight");
                                    overscanRight = XposedHelpers.getIntField(param.thisObject, "mOverscanBottom");
                                    overscanBottom = XposedHelpers.getIntField(param.thisObject, "mOverscanLeft");
                                    break;
                                case Surface.ROTATION_180:
                                    overscanLeft = XposedHelpers.getIntField(param.thisObject, "mOverscanRight");
                                    overscanTop = XposedHelpers.getIntField(param.thisObject, "mOverscanBottom");
                                    overscanRight = XposedHelpers.getIntField(param.thisObject, "mOverscanLeft");
                                    overscanBottom = XposedHelpers.getIntField(param.thisObject, "mOverscanTop");
                                    break;
                                case Surface.ROTATION_270:
                                    overscanLeft = XposedHelpers.getIntField(param.thisObject, "mOverscanBottom");
                                    overscanTop = XposedHelpers.getIntField(param.thisObject, "mOverscanLeft");
                                    overscanRight = XposedHelpers.getIntField(param.thisObject, "mOverscanTop");
                                    overscanBottom = XposedHelpers.getIntField(param.thisObject, "mOverscanRight");
                                    break;
                                default:
                                    overscanLeft = XposedHelpers.getIntField(param.thisObject, "mOverscanLeft");
                                    overscanTop = XposedHelpers.getIntField(param.thisObject, "mOverscanTop");
                                    overscanRight = XposedHelpers.getIntField(param.thisObject, "mOverscanRight");
                                    overscanBottom = XposedHelpers.getIntField(param.thisObject, "mOverscanBottom");
                                    break;
                            }
                        }

                        final boolean navbarOnBottom = XposedHelpers.getBooleanField(param.thisObject, "mNavigationBarOnBottom");
                        final int displayWidth = (Integer)param.args[1];
                        final int displayHeight = (Integer)param.args[2];
                        if (navbarOnBottom) {
                            XposedHelpers.setIntField(param.thisObject, "mDockBottom", 
                                    (displayHeight - overscanBottom));
                            XposedHelpers.setIntField(param.thisObject, "mRestrictedScreenHeight",
                                    (displayHeight - overscanTop - overscanBottom));
                            if (Build.VERSION.SDK_INT > 17) {
                                XposedHelpers.setIntField(param.thisObject, "mRestrictedOverscanScreenHeight",
                                        displayHeight);
                            }
                            XposedHelpers.setIntField(param.thisObject, "mSystemBottom", displayHeight);
                            XposedHelpers.setIntField(param.thisObject, "mContentBottom", 
                                    (displayHeight - overscanBottom));
                            XposedHelpers.setIntField(param.thisObject, "mCurBottom", 
                                    (displayHeight - overscanBottom));
                        } else {
                            XposedHelpers.setIntField(param.thisObject, "mDockRight", 
                                    (displayWidth - overscanRight));
                            XposedHelpers.setIntField(param.thisObject, "mRestrictedScreenWidth", 
                                    (displayWidth - overscanLeft - overscanRight));
                            if (Build.VERSION.SDK_INT > 17) {
                                XposedHelpers.setIntField(param.thisObject, "mRestrictedOverscanScreenWidth",
                                        displayWidth);
                            }
                            XposedHelpers.setIntField(param.thisObject, "mSystemRight", displayWidth);
                            XposedHelpers.setIntField(param.thisObject, "mContentRight", 
                                    (displayWidth - overscanRight));
                            XposedHelpers.setIntField(param.thisObject, "mCurRight", 
                                    (displayWidth - overscanRight));
                        }
                        Object tmpNavFrame = XposedHelpers.getObjectField(param.thisObject, "mTmpNavigationFrame");
                        if (Build.VERSION.SDK_INT > 17) {
                            XposedHelpers.callMethod(navigationBar, "computeFrameLw", 
                                    tmpNavFrame, tmpNavFrame, tmpNavFrame, tmpNavFrame, tmpNavFrame);
                        } else {
                            XposedHelpers.callMethod(navigationBar, "computeFrameLw", 
                                    tmpNavFrame, tmpNavFrame, tmpNavFrame, tmpNavFrame);
                        }
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static boolean shouldHideNavigationBarLw(int systemUiVisibility) {
        if (expandedDesktopHidesNavigationBar()) {
            return true;
        }

        if (mPhoneWindowManager != null
                && XposedHelpers.getBooleanField(mPhoneWindowManager, "mCanHideNavigationBar")) {
            if ((systemUiVisibility & View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) != 0) {
                return true;
            }
        }

        return false;
    }

    private static boolean expandedDesktopHidesStatusbar() {
        return (mExpandedDesktop
                && (mExpandedDesktopMode & GravityBoxSettings.ED_STATUSBAR) != 0);
    }

    private static boolean expandedDesktopHidesNavigationBar() {
        return (mExpandedDesktop
                && (mExpandedDesktopMode & GravityBoxSettings.ED_NAVBAR) != 0);
    }

    private static class ApplicationDisplayMetrics {
        boolean rotated;
        int dh;
        int dw;
        int appWidth;
        int appHeight;
    }

    private static void updateDisplayMetrics(Object windowManager) {
        if (windowManager == null || Build.VERSION.SDK_INT < 17
                || Build.VERSION.SDK_INT > 18) return;

        final long origId = Binder.clearCallingIdentity();
        boolean changed = false;

        final Object windowMap = XposedHelpers.getObjectField(windowManager, "mWindowMap");
        synchronized (windowMap) {
            final Object displayContent = XposedHelpers.callMethod(windowManager, "getDefaultDisplayContentLocked");
            final Object displayInfo = displayContent != null ?
                    XposedHelpers.callMethod(displayContent, "getDisplayInfo") : null;
            final int oldWidth = displayInfo != null ? XposedHelpers.getIntField(displayInfo, "appWidth") : -1;
            final int oldHeight = displayInfo != null ? XposedHelpers.getIntField(displayInfo, "appHeight") : -1;
            final ApplicationDisplayMetrics metrics = 
                    updateApplicationDisplayMetricsLocked(windowManager, displayContent);

            if (metrics != null && oldWidth >= 0  && oldHeight >= 0) {
                changed = oldWidth != metrics.appWidth || oldHeight != metrics.appHeight;
            }

            if (changed) {
                if (DEBUG ) log("Sending new configuration");
                Handler h = (Handler) XposedHelpers.getObjectField(windowManager, "mH");
                h.sendEmptyMessage(SEND_NEW_CONFIGURATION);
            }

            Binder.restoreCallingIdentity(origId);
        }
    }

    private static ApplicationDisplayMetrics updateApplicationDisplayMetricsLocked(
            Object windowManager, Object displayContent) {
        if (!XposedHelpers.getBooleanField(windowManager, "mDisplayReady")) {
            return null;
        }

        final ApplicationDisplayMetrics m = calculateDisplayMetrics(windowManager, displayContent);
        final Object displayInfo = XposedHelpers.callMethod(displayContent, "getDisplayInfo");
        final Object policy = XposedHelpers.getObjectField(windowManager, "mPolicy");
        final int rotation = XposedHelpers.getIntField(windowManager, "mRotation");
        
        m.appWidth = (Integer) XposedHelpers.callMethod(policy, "getNonDecorDisplayWidth", m.dw, m.dh, rotation);
        m.appHeight = (Integer) XposedHelpers.callMethod(policy, "getNonDecorDisplayHeight", m.dw, m.dh, rotation);

        synchronized(XposedHelpers.getObjectField(displayContent, "mDisplaySizeLock")) {
            XposedHelpers.setIntField(displayInfo, "rotation", rotation);
            XposedHelpers.setIntField(displayInfo, "logicalWidth", m.dw);
            XposedHelpers.setIntField(displayInfo, "logicalHeight", m.dh);
            XposedHelpers.setIntField(displayInfo, "logicalDensityDpi",
                    XposedHelpers.getIntField(displayContent, "mBaseDisplayDensity"));
            XposedHelpers.setIntField(displayInfo, "appWidth", m.appWidth);
            XposedHelpers.setIntField(displayInfo, "appHeight", m.appHeight);
            Class<?>[] params = new Class<?>[2];
            params[0] = DisplayMetrics.class;
            params[1] = XposedHelpers.findClass("android.view.CompatibilityInfoHolder", null);
            XposedHelpers.callMethod(displayInfo, "getLogicalMetrics", params,
                    XposedHelpers.getObjectField(windowManager, "mRealDisplayMetrics"), null);
            XposedHelpers.callMethod(displayInfo, "getAppMetrics", params,
                    XposedHelpers.getObjectField(windowManager, "mDisplayMetrics"), null);
            Object displayManagerService = XposedHelpers.getObjectField(windowManager, "mDisplayManagerService");
            XposedHelpers.callMethod(displayManagerService, "setDisplayInfoOverrideFromWindowManager",
                    XposedHelpers.callMethod(displayContent, "getDisplayId"), displayInfo);
 
            if (Build.VERSION.SDK_INT < 18) {
                Object animator = XposedHelpers.getObjectField(windowManager, "mAnimator");
                XposedHelpers.callMethod(animator, "setDisplayDimensions", m.dw, m.dh, m.appWidth, m.appHeight);
            }
        }

        if (DEBUG) log("updateApplicationDisplayMetricsLocked: m=" + m.toString());
        return m;
    }

    private static ApplicationDisplayMetrics calculateDisplayMetrics(
            Object windowManager, Object displayContent) {
        final ApplicationDisplayMetrics dm = new ApplicationDisplayMetrics();

        final int rotation = XposedHelpers.getIntField(windowManager, "mRotation");
        dm.rotated = (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270);
        final int realdw = dm.rotated ?
                XposedHelpers.getIntField(displayContent, "mBaseDisplayHeight") :
                    XposedHelpers.getIntField(displayContent, "mBaseDisplayWidth");
        final int realdh = dm.rotated ?
                XposedHelpers.getIntField(displayContent, "mBaseDisplayWidth") :
                    XposedHelpers.getIntField(displayContent, "mBaseDisplayHeight");

        dm.dw = realdw;
        dm.dh = realdh;

        if(XposedHelpers.getBooleanField(windowManager, "mAltOrientation")) {
            if (realdw > realdh) {
                // Turn landscape into portrait.
                int maxw = (int)(realdh/1.3f);
                if (maxw < realdw) {
                    dm.dw = maxw;
                }
            } else {
                // Turn portrait into landscape.
                int maxh = (int)(realdw/1.3f);
                if (maxh < realdh) {
                    dm.dh = maxh;
                }
            }
        }

        return dm;
    }
}
