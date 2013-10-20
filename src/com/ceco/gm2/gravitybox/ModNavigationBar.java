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
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
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

    private static boolean mAlwaysShowMenukey;
    private static Object mNavigationBarView;
    private static Object[] mRecentsKeys;
    private static HomeKeyInfo[] mHomeKeys;
    private static int mRecentsSingletapAction = 0;
    private static int mRecentsLongpressAction = 0;
    private static int mHomeLongpressAction = 0;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    static class HomeKeyInfo {
        public ImageView homeKey;
        public boolean supportsLongPressDefault;
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

            XposedBridge.hookAllConstructors(navbarViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    if (context == null) return;

                    mNavigationBarView = param.thisObject;
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_NAVBAR_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
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
                    final Resources res = ((View) param.thisObject).getContext().getResources();
                    final int backButtonResId = res.getIdentifier("back", "id", PACKAGE_NAME);
                    final int recentAppsResId = res.getIdentifier("recent_apps", "id", PACKAGE_NAME);
                    final int homeButtonResId = res.getIdentifier("home", "id", PACKAGE_NAME);
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
                    updateRecentsKeyCode();
                    updateHomeKeyLongpressSupport();
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
        } catch(Throwable t) {
            XposedBridge.log(t);
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
        return (mRecentsSingletapAction != 0 || mRecentsLongpressAction != 0);
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
}
