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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class StatusbarSignalCluster implements BroadcastSubReceiver {
    public static final String TAG = "GB:StatusbarSignalCluster";

    protected LinearLayout mView;
    protected StatusBarIconManager mIconManager;
    protected Resources mResources;
    protected boolean mWifiIconLayoutUpdated;

    protected static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static StatusbarSignalCluster create(LinearLayout view, StatusBarIconManager iconManager) {
        if (Utils.isMtkDevice()) {
            return new StatusbarSignalClusterMtk(view, iconManager);
        } else {
            return new StatusbarSignalCluster(view, iconManager);
        }
    }

    public StatusbarSignalCluster(LinearLayout view, StatusBarIconManager iconManager) {
        mView = view;
        mIconManager = iconManager;
        mResources = mView.getResources();
        mWifiIconLayoutUpdated = false;

        if (mView != null) {
            try {
                XposedHelpers.findAndHookMethod(mView.getClass(), "apply", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        apply();
                    }
                });
            } catch (Throwable t) {
                log("Error hooking apply() method: " + t.getMessage());
            }
        }
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) { }

    public void initPreferences(XSharedPreferences prefs) { }

    public void update() {
        if (mView != null) {
            try {
                XposedHelpers.callMethod(mView, "apply");
            } catch (Throwable t) {
                log("Error invoking apply() method: " + t.getMessage());
            }
        }
    }

    protected void apply() {
        try {
            if (XposedHelpers.getObjectField(mView, "mWifiGroup") != null) {
                if (mIconManager.isColoringEnabled()) {
                    updateWiFiIcon();
                    if (!XposedHelpers.getBooleanField(mView, "mIsAirplaneMode")) {
                        updateMobileIcon();
                    }
                }
                updateAirplaneModeIcon();
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    protected void updateWiFiIcon() {
        try {
            if (XposedHelpers.getBooleanField(mView, "mWifiVisible") &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView wifiIcon = (ImageView) XposedHelpers.getObjectField(mView, "mWifi");
                if (wifiIcon != null) {
                    if (!mWifiIconLayoutUpdated) {
                        final int pTop = 
                            Math.round(wifiIcon.getContext().getResources().getDisplayMetrics().density * 1);
                        wifiIcon.setPadding(wifiIcon.getPaddingLeft(), pTop, wifiIcon.getPaddingRight(),
                                wifiIcon.getPaddingBottom());
                        mWifiIconLayoutUpdated = true;
                    }
                    int resId = XposedHelpers.getIntField(mView, "mWifiStrengthId");
                    Drawable d = mIconManager.getWifiIcon(resId);
                    if (d != null) wifiIcon.setImageDrawable(d);
                }
                ImageView wifiActivity = (ImageView) XposedHelpers.getObjectField(mView, "mWifiActivity");
                if (wifiActivity != null) {
                    try {
                        int resId = XposedHelpers.getIntField(mView, "mWifiActivityId");
                        Drawable d = mResources.getDrawable(resId).mutate();
                        d = mIconManager.applyDataActivityColorFilter(d);
                        wifiActivity.setImageDrawable(d);
                    } catch (Resources.NotFoundException e) {
                        wifiActivity.setImageDrawable(null);
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    protected void updateMobileIcon() {
        try {
            if (XposedHelpers.getBooleanField(mView, "mMobileVisible") &&
                    mIconManager.getSignalIconMode() != StatusBarIconManager.SI_MODE_DISABLED) {
                ImageView mobile = (ImageView) XposedHelpers.getObjectField(mView, "mMobile");
                if (mobile != null) {
                    int resId = XposedHelpers.getIntField(mView, "mMobileStrengthId");
                    Drawable d = mIconManager.getMobileIcon(resId);
                    if (d != null) mobile.setImageDrawable(d);
                }
                if (mIconManager.isMobileIconChangeAllowed()) {
                    ImageView mobileActivity = 
                            (ImageView) XposedHelpers.getObjectField(mView, "mMobileActivity");
                    if (mobileActivity != null) {
                        try {
                            int resId = XposedHelpers.getIntField(mView, "mMobileActivityId");
                            Drawable d = mResources.getDrawable(resId).mutate();
                            d = mIconManager.applyDataActivityColorFilter(d);
                            mobileActivity.setImageDrawable(d);
                        } catch (Resources.NotFoundException e) { 
                            mobileActivity.setImageDrawable(null);
                        }
                    }
                    ImageView mobileType = (ImageView) XposedHelpers.getObjectField(mView, "mMobileType");
                    if (mobileType != null) {
                        try {
                            int resId = XposedHelpers.getIntField(mView, "mMobileTypeId");
                            Drawable d = mResources.getDrawable(resId).mutate();
                            d = mIconManager.applyColorFilter(d);
                            mobileType.setImageDrawable(d);
                        } catch (Resources.NotFoundException e) { 
                            mobileType.setImageDrawable(null);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    protected void updateAirplaneModeIcon() {
        try {
            ImageView airplaneModeIcon = (ImageView) XposedHelpers.getObjectField(mView, "mAirplane");
            if (airplaneModeIcon != null) {
                Drawable d = airplaneModeIcon.getDrawable();
                if (mIconManager.isColoringEnabled()) {
                    d = mIconManager.applyColorFilter(d);
                } else if (d != null) {
                    d.setColorFilter(null);
                }
                airplaneModeIcon.setImageDrawable(d);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
