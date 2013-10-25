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

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public class StatusBarIconManager {
    private static final String TAG = "GB:StatusBarIconManager";
    private static final boolean DEBUG = false;
    public static final int DEFAULT_DATA_ACTIVITY_COLOR = Color.WHITE;

    public static final int SI_MODE_GB = 0;
    public static final int SI_MODE_STOCK = 1;
    public static final int SI_MODE_DISABLED = 2;

    private Resources mResources;
    private Resources mSystemUiRes;
    private int mIconColor;
    private int mDataActivityColor;
    private Map<String, Integer> mWifiIconIds;
    private Map<String, Integer> mMobileIconIds;
    private Map<String, Integer> mBatteryIconIds;
    private Map<String, SoftReference<Drawable>> mIconCache;
    private Integer mStockBatteryColor;
    private Integer mDefaultClockColor;
    private Integer mDefaultBatteryPercentageColor;
    private boolean mFollowStockBatteryColor;
    private int mSignalIconMode;
    private boolean mAllowMobileIconChange;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public StatusBarIconManager(Resources res) {
        mResources = res;
        mIconColor = getDefaultIconColor();
        mDataActivityColor = DEFAULT_DATA_ACTIVITY_COLOR;
        mFollowStockBatteryColor = false;
        mSignalIconMode = SI_MODE_GB;
        mAllowMobileIconChange = true;

        Map<String, Integer> tmpMap = new HashMap<String, Integer>();
        tmpMap.put("stat_sys_wifi_signal_0", R.drawable.stat_sys_wifi_signal_0);
        tmpMap.put("stat_sys_wifi_signal_1", R.drawable.stat_sys_wifi_signal_1);
        tmpMap.put("stat_sys_wifi_signal_1_fully", R.drawable.stat_sys_wifi_signal_1_fully);
        tmpMap.put("stat_sys_wifi_signal_2", R.drawable.stat_sys_wifi_signal_2);
        tmpMap.put("stat_sys_wifi_signal_2_fully", R.drawable.stat_sys_wifi_signal_2_fully);
        tmpMap.put("stat_sys_wifi_signal_3", R.drawable.stat_sys_wifi_signal_3);
        tmpMap.put("stat_sys_wifi_signal_3_fully", R.drawable.stat_sys_wifi_signal_3_fully);
        tmpMap.put("stat_sys_wifi_signal_4", R.drawable.stat_sys_wifi_signal_4);
        tmpMap.put("stat_sys_wifi_signal_4_fully", R.drawable.stat_sys_wifi_signal_4_fully);
        tmpMap.put("stat_sys_wifi_signal_null", R.drawable.stat_sys_wifi_signal_null);
        mWifiIconIds = Collections.unmodifiableMap(tmpMap);

        if (Utils.isMtkDevice()) {
            tmpMap = new HashMap<String, Integer>();
            tmpMap.put("stat_sys_gemini_signal_1_blue", R.drawable.stat_sys_signal_1_fully);
            tmpMap.put("stat_sys_gemini_signal_2_blue", R.drawable.stat_sys_signal_2_fully);
            tmpMap.put("stat_sys_gemini_signal_3_blue", R.drawable.stat_sys_signal_3_fully);
            tmpMap.put("stat_sys_gemini_signal_4_blue", R.drawable.stat_sys_signal_4_fully);
            mMobileIconIds = Collections.unmodifiableMap(tmpMap);
        } else {
            tmpMap = new HashMap<String, Integer>();
            tmpMap.put("stat_sys_signal_0", R.drawable.stat_sys_signal_0);
            tmpMap.put("stat_sys_signal_0_fully", R.drawable.stat_sys_signal_0_fully);
            tmpMap.put("stat_sys_signal_1", R.drawable.stat_sys_signal_1);
            tmpMap.put("stat_sys_signal_1_fully", R.drawable.stat_sys_signal_1_fully);
            tmpMap.put("stat_sys_signal_2", R.drawable.stat_sys_signal_2);
            tmpMap.put("stat_sys_signal_2_fully", R.drawable.stat_sys_signal_2_fully);
            tmpMap.put("stat_sys_signal_3", R.drawable.stat_sys_signal_3);
            tmpMap.put("stat_sys_signal_3_fully", R.drawable.stat_sys_signal_3_fully);
            tmpMap.put("stat_sys_signal_4", R.drawable.stat_sys_signal_4);
            tmpMap.put("stat_sys_signal_4_fully", R.drawable.stat_sys_signal_4_fully);
            mMobileIconIds = Collections.unmodifiableMap(tmpMap);
        }

        tmpMap = new HashMap<String, Integer>();
        tmpMap.put("stat_sys_battery_0", R.drawable.stat_sys_battery_0);
        tmpMap.put("stat_sys_battery_15", R.drawable.stat_sys_battery_15);
        tmpMap.put("stat_sys_battery_35", R.drawable.stat_sys_battery_28);
        tmpMap.put("stat_sys_battery_49", R.drawable.stat_sys_battery_43);
        tmpMap.put("stat_sys_battery_60", R.drawable.stat_sys_battery_57);
        tmpMap.put("stat_sys_battery_75", R.drawable.stat_sys_battery_71);
        tmpMap.put("stat_sys_battery_90", R.drawable.stat_sys_battery_85);
        tmpMap.put("stat_sys_battery_100", R.drawable.stat_sys_battery_100);
        tmpMap.put("stat_sys_battery_charge_anim0", R.drawable.stat_sys_battery_charge_anim0);
        tmpMap.put("stat_sys_battery_charge_anim15", R.drawable.stat_sys_battery_charge_anim15);
        tmpMap.put("stat_sys_battery_charge_anim35", R.drawable.stat_sys_battery_charge_anim28);
        tmpMap.put("stat_sys_battery_charge_anim49", R.drawable.stat_sys_battery_charge_anim43);
        tmpMap.put("stat_sys_battery_charge_anim60", R.drawable.stat_sys_battery_charge_anim57);
        tmpMap.put("stat_sys_battery_charge_anim75", R.drawable.stat_sys_battery_charge_anim71);
        tmpMap.put("stat_sys_battery_charge_anim90", R.drawable.stat_sys_battery_charge_anim85);
        tmpMap.put("stat_sys_battery_charge_anim100", R.drawable.stat_sys_battery_charge_anim100);
        mBatteryIconIds = Collections.unmodifiableMap(tmpMap);

        mIconCache = new HashMap<String, SoftReference<Drawable>>();
    }

    public int getDefaultIconColor() {
        if (mFollowStockBatteryColor && mStockBatteryColor != null) {
            return mStockBatteryColor;
        } else {
            return mResources.getColor(android.R.color.holo_blue_dark);
        }
    }

    public void initStockBatteryColor(Context context) {
        try {
            final Resources res = context.getResources();
            final int resId = res.getIdentifier(
                    "stat_sys_battery_100", "drawable", "com.android.systemui");
            if (resId != 0) {
                final Bitmap b = BitmapFactory.decodeResource(res, resId);
                final int x = b.getWidth() / 2;
                final int y = b.getHeight() / 2;
                mStockBatteryColor = b.getPixel(x, y);
            }
            if (DEBUG) log("mStockBatteryColor = " + 
                    ((mStockBatteryColor != null ) ? Integer.toHexString(mStockBatteryColor) : "NULL"));
        } catch (Throwable t) {
            log("Error initializing stock battery color: " + t.getMessage());
        }
    }

    public void setSystemUiResources(Resources res) {
        mSystemUiRes = res;
    }

    public void setSignalIconMode(int mode) {
        mSignalIconMode = mode;
        clearCache();
    }

    public int getSignalIconMode() {
        return mSignalIconMode;
    }

    public void setFollowStockBatteryColor(boolean follow) {
        mFollowStockBatteryColor = follow;
    }

    public void setDefaultClockColor(int color) {
        mDefaultClockColor = color;
    }

    public Integer getDefaultClockColor() {
        return mDefaultClockColor;
    }

    public int getClockColor() {
        if (mFollowStockBatteryColor && mStockBatteryColor != null) {
            return mStockBatteryColor;
        } else {
            return (mDefaultClockColor != null ?
                    mDefaultClockColor : getDefaultIconColor());
        }
    }

    public void setDefaultBatteryPercentageColor(int color) {
        mDefaultBatteryPercentageColor = color;
    }

    public Integer getDefaultBatteryPercentageColor() {
        return mDefaultBatteryPercentageColor;
    }

    public int getBatteryPercentageColor() {
        if (mFollowStockBatteryColor && mStockBatteryColor != null) {
            return mStockBatteryColor;
        } else {
            return (mDefaultBatteryPercentageColor != null ?
                    mDefaultBatteryPercentageColor : getDefaultIconColor());
        }
    }

    public int getIconColor() {
        return mIconColor;
    }

    public int getDataActivityColor() {
        return mDataActivityColor;
    }

    public void setIconColor(int color) {
        mIconColor = color;
        clearCache();
    }

    public void setDataActivityColor(int color) {
        mDataActivityColor = color;
    }

    public Drawable applyColorFilter(Drawable drawable) {
        drawable.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    public Drawable applyDataActivityColorFilter(Drawable drawable) {
        drawable.setColorFilter(mDataActivityColor, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    public void clearCache() {
        mIconCache.clear();
        if (DEBUG) log("Cache cleared");
    }

    private Drawable getCachedDrawable(String key) {
        if (mIconCache.containsKey(key)) {
            if (DEBUG) log("getCachedDrawable('" + key + "') - cached drawable found");
            return mIconCache.get(key).get();
        }
        return null;
    }

    private void setCachedDrawable(String key, Drawable d) {
        mIconCache.put(key, new SoftReference<Drawable>(d));
        if (DEBUG) log("setCachedDrawable('" + key + "') - storing to cache");
    }

    public Drawable getWifiIcon(int resId) {
        Drawable cd;
        String key;

        try {
            key = mSystemUiRes.getResourceEntryName(resId);
        } catch (Resources.NotFoundException nfe) {
            return null;
        }

        switch(mSignalIconMode) {
            case SI_MODE_GB:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                if (mWifiIconIds.containsKey(key)) {
                    Drawable d = mResources.getDrawable(mWifiIconIds.get(key)).mutate();
                    d = applyColorFilter(d);
                    setCachedDrawable(key, d);
                    return d;
                }
                if (DEBUG) log("getWifiIcon: no drawable for key: " + key);
                return null;

            case SI_MODE_STOCK:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                Drawable d = mSystemUiRes.getDrawable(resId).mutate();
                d = applyColorFilter(d);
                setCachedDrawable(key, d);
                return d;

            case SI_MODE_DISABLED:
            default:
                return null;
        }
    }

    public Drawable getMobileIcon(int resId) {
        Drawable cd;
        String key;

        try {
            key = mSystemUiRes.getResourceEntryName(resId);
        } catch (Resources.NotFoundException nfe) {
            return null;
        }

        mAllowMobileIconChange = key.contains("blue") || !Utils.isMtkDevice();
        if (!mAllowMobileIconChange) {
            return null;
        }

        switch(mSignalIconMode) {
            case SI_MODE_GB:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                if (mMobileIconIds.containsKey(key)) {
                    Drawable d = mResources.getDrawable(mMobileIconIds.get(key)).mutate();
                    d = applyColorFilter(d);
                    setCachedDrawable(key, d);
                    return d;
                }
                if (DEBUG) log("getMobileIcon: no drawable for key: " + key);
                return null;

            case SI_MODE_STOCK:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                Drawable d = mSystemUiRes.getDrawable(resId).mutate();
                d = applyColorFilter(d);
                setCachedDrawable(key, d);
                return d;

            case SI_MODE_DISABLED:
            default:
                return null;
        }
    }

    public boolean isMobileIconChangeAllowed() {
        return mAllowMobileIconChange;
    }

    public Drawable getBatteryIcon(int level, boolean plugged) {
        String key = getKeyForBatteryStatus(level, plugged);

        Drawable cd = getCachedDrawable(key);
        if (cd != null) return cd;

        if (mBatteryIconIds.containsKey(key)) {
            Drawable d = mResources.getDrawable(mBatteryIconIds.get(key)).mutate();
            d = applyColorFilter(d);
            setCachedDrawable(key, d);
            return d;
        }

        if (DEBUG) log("getBatteryIcon: no drawable for key: " + key);
        return null;
    }

    private String getKeyForBatteryStatus(int level, boolean plugged) {
        String key = plugged ? "stat_sys_battery_charge_anim" : "stat_sys_battery_";

        if (level <= 4) {
            key += "0";
        } else if (level <= 15) {
            key += "15";
        } else if (level <= 35) {
            key += "35";
        } else if (level <= 49) {
            key += "49";
        } else if (level <= 60) {
            key += "60";
        } else if (level <= 75) {
            key += "75";
        } else if (level <= 90) {
            key += "90";
        } else if (level <= 100) {
            key += "100";
        }

        return key;
    }
}
