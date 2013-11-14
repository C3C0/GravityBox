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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModNavigationBar {
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "GB:ModNavigationBar";
    private static final boolean DEBUG = false;

    private static final String CLASS_NAVBAR_VIEW = "com.android.systemui.statusbar.phone.NavigationBarView";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_KEY_BUTTON_VIEW = "com.android.systemui.statusbar.policy.KeyButtonView";

    private static boolean mAlwaysShowMenukey;
    private static View mNavigationBarView;
    private static Object[] mRecentsKeys;
    private static HomeKeyInfo[] mHomeKeys;
    private static int mRecentsSingletapAction = 0;
    private static int mRecentsLongpressAction = 0;
    private static int mHomeLongpressAction = 0;
    private static boolean mHwKeysEnabled;

    // Application launcher key
    private static boolean mAppLauncherEnabled;
    private static Resources mResources;
    private static Context mGbContext;
    private static AppLauncher mAppLauncher;
    private static NavbarViewInfo[] mNavbarViewInfo = new NavbarViewInfo[2];

    // Colors
    private static boolean mNavbarColorsEnabled;
    private static int mKeyDefaultColor = 0xe8ffffff;
    private static int mKeyDefaultGlowColor = 0x40ffffff;
    private static int mNavbarDefaultBgColor = 0xff000000;
    private static int mKeyColor;
    private static int mKeyGlowColor;
    private static int mNavbarBgColor;
    private static Integer mNavbarBgColorOriginal;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    static class HomeKeyInfo {
        public ImageView homeKey;
        public boolean supportsLongPressDefault;
    }

    static class NavbarViewInfo {
        ViewGroup navButtons;
        View originalView;
        KeyButtonView appLauncherView;
        int position;
        boolean visible;
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_MENUKEY)) {
                    mAlwaysShowMenukey = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_MENUKEY, false);
                    if (DEBUG) log("mAlwaysShowMenukey = " + mAlwaysShowMenukey);
                    if (mNavigationBarView != null) {
                        try {
                            final boolean showMenu = XposedHelpers.getBooleanField(
                                    mNavigationBarView, "mShowMenu");
                            XposedHelpers.callMethod(mNavigationBarView, 
                                    "setMenuVisibility", showMenu, true);
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                        }
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_LAUNCHER_ENABLE)) {
                    mAppLauncherEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_LAUNCHER_ENABLE, false);
                    setAppKeyVisibility(mAppLauncherEnabled);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_KEY_COLOR)) {
                    mKeyColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_KEY_COLOR, mKeyDefaultColor);
                    setKeyColor();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_KEY_GLOW_COLOR)) {
                    mKeyGlowColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_KEY_GLOW_COLOR, mKeyDefaultGlowColor);
                    setKeyColor();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_BG_COLOR)) {
                    mNavbarBgColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_BG_COLOR, mNavbarDefaultBgColor);
                    setNavbarBgColor();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_NAVBAR_COLOR_ENABLE)) {
                    mNavbarColorsEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_NAVBAR_COLOR_ENABLE, false);
                    setNavbarBgColor();
                    setKeyColor();
                }
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED)) {
                mRecentsSingletapAction = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, 0);
                updateRecentsKeyCode();
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED)) {
                mRecentsLongpressAction = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, 0);
                updateRecentsKeyCode();
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED)) {
                mHomeLongpressAction = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, 0);
                updateHomeKeyLongpressSupport();
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_PIE_CHANGED) && 
                    intent.hasExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE)) {
                mHwKeysEnabled = !intent.getBooleanExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE, false);
                updateRecentsKeyCode();
            }
        }
    };

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> navbarViewClass = XposedHelpers.findClass(CLASS_NAVBAR_VIEW, classLoader);
            final Class<?> phoneStatusbarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);

            mAlwaysShowMenukey = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_MENUKEY, false);

            try {
                mRecentsSingletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_SINGLETAP, "0"));
                mRecentsLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_LONGPRESS, "0"));
                mHomeLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_HOME_LONGPRESS, "0"));
            } catch (NumberFormatException nfe) {
                XposedBridge.log(nfe);
            }

            mAppLauncherEnabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_NAVBAR_LAUNCHER_ENABLE, false);
            mHwKeysEnabled = !prefs.getBoolean(GravityBoxSettings.PREF_KEY_HWKEYS_DISABLE, false);

            XposedBridge.hookAllConstructors(navbarViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    if (context == null) return;

                    mResources = context.getResources();

                    mGbContext = context.createPackageContext(
                            GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                    final Resources res = mGbContext.getResources();
                    mNavbarColorsEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_COLOR_ENABLE, false);
                    mKeyDefaultColor = res.getColor(R.color.navbar_key_color);
                    mKeyColor = prefs.getInt(GravityBoxSettings.PREF_KEY_NAVBAR_KEY_COLOR, mKeyDefaultColor);
                    mKeyDefaultGlowColor = res.getColor(R.color.navbar_key_glow_color);
                    mKeyGlowColor = prefs.getInt(
                            GravityBoxSettings.PREF_KEY_NAVBAR_KEY_GLOW_COLOR, mKeyDefaultGlowColor);
                    mNavbarDefaultBgColor = res.getColor(R.color.navbar_bg_color);
                    mNavbarBgColor = prefs.getInt(
                            GravityBoxSettings.PREF_KEY_NAVBAR_BG_COLOR, mNavbarDefaultBgColor);

                    mAppLauncher = new AppLauncher(context, prefs);

                    mNavigationBarView = (View) param.thisObject;
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_PIE_CHANGED);
                    context.registerReceiver(mBroadcastReceiver, intentFilter);
                    if (DEBUG) log("NavigationBarView constructed; Broadcast receiver registered");
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "setMenuVisibility",
                    boolean.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = (Boolean) param.args[0] || mAlwaysShowMenukey;
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Context context = ((View) param.thisObject).getContext();
                    final Resources gbRes = mGbContext.getResources();
                    final int backButtonResId = mResources.getIdentifier("back", "id", PACKAGE_NAME);
                    final int recentAppsResId = mResources.getIdentifier("recent_apps", "id", PACKAGE_NAME);
                    final int homeButtonResId = mResources.getIdentifier("home", "id", PACKAGE_NAME);
                    final View[] rotatedViews = 
                            (View[]) XposedHelpers.getObjectField(param.thisObject, "mRotatedViews");

                    if (rotatedViews != null) {
                        mRecentsKeys = new Object[rotatedViews.length];
                        mHomeKeys = new HomeKeyInfo[rotatedViews.length];
                        int index = 0;
                        for(View v : rotatedViews) {
                            if (backButtonResId != 0) { 
                                ImageView backButton = (ImageView) v.findViewById(backButtonResId);
                                if (backButton != null) {
                                    backButton.setScaleType(ScaleType.FIT_CENTER);
                                }
                            }
                            if (recentAppsResId != 0) {
                                ImageView recentAppsButton = (ImageView) v.findViewById(recentAppsResId);
                                mRecentsKeys[index] = recentAppsButton;
                            }
                            if (homeButtonResId != 0) { 
                                HomeKeyInfo hkInfo = new HomeKeyInfo();
                                hkInfo.homeKey = (ImageView) v.findViewById(homeButtonResId);
                                if (hkInfo.homeKey != null) {
                                    hkInfo.supportsLongPressDefault = 
                                        XposedHelpers.getBooleanField(hkInfo.homeKey, "mSupportsLongpress");
                                }
                                mHomeKeys[index] = hkInfo;
                            }
                            index++;
                        }
                    }

                    // insert app key
                    ViewGroup vRot, navButtons;

                    // insert app key in rot0 view
                    vRot = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(
                            mResources.getIdentifier("rot0", "id", PACKAGE_NAME));
                    if (vRot != null) {
                        KeyButtonView appKey = new KeyButtonView(context);
                        appKey.setScaleType(ScaleType.FIT_CENTER);
                        appKey.setClickable(true);
                        appKey.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_apps));
                        appKey.setOnClickListener(mAppKeyOnClickListener);
                        navButtons = (ViewGroup) vRot.findViewById(
                                mResources.getIdentifier("nav_buttons", "id", PACKAGE_NAME));
                        prepareNavbarViewInfo(navButtons, 0, appKey);
                    }

                    // insert app key in rot90 view
                    vRot = (ViewGroup) ((ViewGroup) param.thisObject).findViewById(
                            mResources.getIdentifier("rot90", "id", PACKAGE_NAME));
                    if (vRot != null) {
                        KeyButtonView appKey = new KeyButtonView(context);
                        appKey.setClickable(true);
                        appKey.setImageDrawable(gbRes.getDrawable(R.drawable.ic_sysbar_apps));
                        appKey.setOnClickListener(mAppKeyOnClickListener);
                        navButtons = (ViewGroup) vRot.findViewById(
                                mResources.getIdentifier("nav_buttons", "id", PACKAGE_NAME));
                        prepareNavbarViewInfo(navButtons, 1, appKey);
                    }

                    setAppKeyVisibility(mAppLauncherEnabled);
                    updateRecentsKeyCode();
                    updateHomeKeyLongpressSupport();
                    setNavbarBgColor();
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, 
                    "shouldDisableNavbarGestures", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (mHomeLongpressAction != 0) {
                        param.setResult(true);
                        return;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "setDisabledFlags",
                    int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean visible = mAppLauncherEnabled;
                    View v = (View) XposedHelpers.callMethod(param.thisObject, "getRecentsButton");
                    if (v != null) {
                        visible &= v.getVisibility() == View.VISIBLE;
                    }
                    setAppKeyVisibility(visible);
                }
            });

            XposedHelpers.findAndHookMethod(navbarViewClass, "setNavigationIconHints",
                    int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mNavbarColorsEnabled) {
                        final int navigationIconHints = XposedHelpers.getIntField(
                                param.thisObject, "mNavigationIconHints");
                        if ((Integer) param.args[0] != navigationIconHints || (Boolean)param.args[1]) {
                            setKeyColor();
                        }
                    }
                }
            });
        } catch(Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void prepareNavbarViewInfo(ViewGroup navButtons, int index, KeyButtonView appView) {
        try {
            final int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    40, navButtons.getResources().getDisplayMetrics());
            if (DEBUG) log("App key view minimum size=" + size);

            mNavbarViewInfo[index] = new NavbarViewInfo();
            mNavbarViewInfo[index].navButtons = navButtons;
            mNavbarViewInfo[index].appLauncherView = appView;

            int searchPosition = index == 0 ? 0 : navButtons.getChildCount()-1;
            View v = navButtons.getChildAt(searchPosition);
            if (v.getId() == -1 && !v.getClass().getName().equals(CLASS_KEY_BUTTON_VIEW)) {
                mNavbarViewInfo[index].originalView = v;
            } else {
                searchPosition = searchPosition == 0 ? navButtons.getChildCount()-1 : 0;
                v = navButtons.getChildAt(searchPosition);
                if (v.getId() == -1 && !v.getClass().getName().equals(CLASS_KEY_BUTTON_VIEW)) {
                    mNavbarViewInfo[index].originalView = v;
                }
            }
            mNavbarViewInfo[index].position = searchPosition;

            // determine app key layout
            LinearLayout.LayoutParams lp = null;
            if (mNavbarViewInfo[index].originalView != null) {
                // determine layout from layout of placeholder view we found
                ViewGroup.LayoutParams ovlp = mNavbarViewInfo[index].originalView.getLayoutParams();
                if (DEBUG) log("originalView: lpWidth=" + ovlp.width + "; lpHeight=" + ovlp.height);
                if (ovlp.width >= 0) {
                    lp = new LinearLayout.LayoutParams(size, LinearLayout.LayoutParams.MATCH_PARENT, 0);
                } else if (ovlp.height >= 0) {
                    lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, size, 0);
                } else {
                    log("Weird layout of placeholder view detected");
                }
            } else {
                // determine layout from Back key
                final int resId = navButtons.getResources().getIdentifier("back", "id", PACKAGE_NAME);
                if (resId != 0) {
                    View back = navButtons.findViewById(resId);
                    if (back != null) {
                        ViewGroup.LayoutParams blp = back.getLayoutParams();
                        if (blp.width >= 0) {
                            lp = new LinearLayout.LayoutParams(size, LinearLayout.LayoutParams.MATCH_PARENT, 0);
                        } else if (blp.height >= 0) {
                            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, size, 0);
                        } else {
                            log("Weird layout of back button view detected");
                        }
                    } else {
                        log("Could not find back button view");
                    }
                } else {
                    log("Could not find back button resource ID");
                }
            }
            // worst case scenario (should never happen, but just to make sure)
            if (lp == null) {
                lp = new LinearLayout.LayoutParams(size, size, 0);
            }
            if (DEBUG) log("appView: lpWidth=" + lp.width + "; lpHeight=" + lp.height);
            mNavbarViewInfo[index].appLauncherView.setLayoutParams(lp);
        } catch (Throwable t) {
            log("Error preparing NavbarViewInfo: " + t.getMessage());
        }
    }

    private static void setAppKeyVisibility(boolean visible) {
        try {
            for (int i = 0; i <= 1; i++) {
                if (mNavbarViewInfo[i].visible == visible) continue;

                if (mNavbarViewInfo[i].originalView != null) {
                    mNavbarViewInfo[i].navButtons.removeViewAt(mNavbarViewInfo[i].position);
                    mNavbarViewInfo[i].navButtons.addView(visible ?
                            mNavbarViewInfo[i].appLauncherView : mNavbarViewInfo[i].originalView,
                            mNavbarViewInfo[i].position);
                } else {
                    if (visible) {
                        mNavbarViewInfo[i].navButtons.addView(mNavbarViewInfo[i].appLauncherView,
                                mNavbarViewInfo[i].position);
                    } else {
                        mNavbarViewInfo[i].navButtons.removeView(mNavbarViewInfo[i].appLauncherView);
                    }
                }
                mNavbarViewInfo[i].visible = visible;
                mNavbarViewInfo[i].navButtons.requestLayout();
                if (DEBUG) log("setAppKeyVisibility: visible=" + visible);
            }
        } catch (Throwable t) {
            log("Error setting app key visibility: " + t.getMessage());
        }
    }

    private static void updateRecentsKeyCode() {
        if (mRecentsKeys == null) return;

        try {
            final boolean hasAction = recentsKeyHasAction();
            for (Object o : mRecentsKeys) {
                if (o != null) {
                    XposedHelpers.setIntField(o, "mCode", hasAction ? KeyEvent.KEYCODE_APP_SWITCH : 0);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static boolean recentsKeyHasAction() {
        return (mRecentsSingletapAction != 0 ||
                mRecentsLongpressAction != 0 ||
                !mHwKeysEnabled);
    }

    private static void updateHomeKeyLongpressSupport() {
        if (mHomeKeys == null) return;

        try {
            for (HomeKeyInfo hkInfo : mHomeKeys) {
                if (hkInfo.homeKey != null) {
                    XposedHelpers.setBooleanField(hkInfo.homeKey, "mSupportsLongpress",
                            mHomeLongpressAction == 0 ? hkInfo.supportsLongPressDefault : true);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static View.OnClickListener mAppKeyOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mAppLauncher != null && mHwKeysEnabled) {
                mAppLauncher.showDialog();
            }
        }
    };

    private static void setKeyColor() {
        try {
            View v = (View) XposedHelpers.getObjectField(mNavigationBarView, "mCurrentView");
            ViewGroup navButtons = (ViewGroup) v.findViewById(
                    mResources.getIdentifier("nav_buttons", "id", PACKAGE_NAME));
            final int childCount = navButtons.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (navButtons.getChildAt(i) instanceof ImageView) {
                    ImageView imgv = (ImageView)navButtons.getChildAt(i);
                    if (mNavbarColorsEnabled) {
                        imgv.setColorFilter(mKeyColor, PorterDuff.Mode.SRC_ATOP);
                    } else {
                        imgv.clearColorFilter();
                    }
                    if (imgv.getClass().getName().equals(CLASS_KEY_BUTTON_VIEW)) {
                        Drawable d = (Drawable) XposedHelpers.getObjectField(imgv, "mGlowBG");
                        if (d != null) {
                            if (mNavbarColorsEnabled) {
                                d.setColorFilter(mKeyGlowColor, PorterDuff.Mode.SRC_ATOP);
                            } else {
                                d.clearColorFilter();
                            }
                        }
                    } else if (imgv instanceof KeyButtonView) {
                        ((KeyButtonView) imgv).setGlowColor(mKeyGlowColor);
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setNavbarBgColor() {
        try {
            if (!mNavbarColorsEnabled) {
                if (mNavbarBgColorOriginal != null) {
                    ColorDrawable cd = new ColorDrawable(mNavbarBgColorOriginal);
                    mNavigationBarView.setBackground(cd);
                    if (DEBUG) log("Restored navbar background original color");
                }
            } else {
                if (mNavbarBgColorOriginal == null && 
                        (mNavigationBarView.getBackground() instanceof ColorDrawable)) {
                    mNavbarBgColorOriginal = 
                            ((ColorDrawable) mNavigationBarView.getBackground()).getColor();
                    if (DEBUG) log("Saved navbar background original color");
                }
                if (Utils.isXperiaDevice()) {
                    if (!(mNavigationBarView.getBackground() instanceof ColorDrawable)) {
                        ColorDrawable colorDrawable = new ColorDrawable(mNavbarBgColor);
                        mNavigationBarView.setBackground(colorDrawable);
                    } else {
                        ((ColorDrawable) mNavigationBarView.getBackground()).setColor(mNavbarBgColor);
                    }
                } else {
                    if (!(mNavigationBarView.getBackground() instanceof BackgroundAlphaColorDrawable)) {
                        BackgroundAlphaColorDrawable colorDrawable = new BackgroundAlphaColorDrawable(mNavbarBgColor);
                        mNavigationBarView.setBackground(colorDrawable);
                    } else {
                        ((BackgroundAlphaColorDrawable) mNavigationBarView.getBackground()).setBgColor(mNavbarBgColor);
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
