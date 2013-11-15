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
    private int[] mIconColor;
    private int[] mDataActivityColor;
    private Map<String, Integer> mWifiIconIds;
    private Map<String, Integer> mMobileIconIds;
    private Map<String, Integer> mBatteryIconIds;
    private Map<String, Integer> mBasicIconIds;
    private Map<String, SoftReference<Drawable>> mIconCache;
    private Integer mStockBatteryColor;
    private Integer mDefaultClockColor;
    private Integer mDefaultBatteryPercentageColor;
    private boolean mFollowStockBatteryColor;
    private int mSignalIconMode;
    private boolean[] mAllowMobileIconChange;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public StatusBarIconManager(Resources res, Resources sysUiRes) {
        mResources = res;
        mSystemUiRes = sysUiRes;
        mIconColor = new int[2];
        mIconColor[0] = mIconColor[1] = getDefaultIconColor();
        mDataActivityColor = new int[] { DEFAULT_DATA_ACTIVITY_COLOR, DEFAULT_DATA_ACTIVITY_COLOR };
        mFollowStockBatteryColor = false;
        mSignalIconMode = SI_MODE_GB;
        mAllowMobileIconChange = new boolean[] { true, true };

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
            tmpMap.put("stat_sys_gemini_signal_1_orange", R.drawable.stat_sys_signal_1_fully);
            tmpMap.put("stat_sys_gemini_signal_2_orange", R.drawable.stat_sys_signal_2_fully);
            tmpMap.put("stat_sys_gemini_signal_3_orange", R.drawable.stat_sys_signal_3_fully);
            tmpMap.put("stat_sys_gemini_signal_4_orange", R.drawable.stat_sys_signal_4_fully);
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

        tmpMap = new HashMap<String, Integer>();
        tmpMap.put("stat_sys_data_bluetooth", R.drawable.stat_sys_data_bluetooth);
        tmpMap.put("stat_sys_data_bluetooth_connected", R.drawable.stat_sys_data_bluetooth_connected);
        tmpMap.put("stat_sys_alarm", null);
        tmpMap.put("stat_sys_ringer_vibrate", null);
        tmpMap.put("stat_sys_headset_with_mic", null);
        tmpMap.put("stat_sys_headset_without_mic", null);
        mBasicIconIds = Collections.unmodifiableMap(tmpMap);

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

    public int getIconColor(int index) {
        return mIconColor[index];
    }

    public int getIconColor() {
        return getIconColor(0);
    }

    public int getDataActivityColor(int index) {
        return mDataActivityColor[index];
    }

    public void setIconColor(int index, int color) {
        mIconColor[index] = color;
        clearCache();
    }

    public void setIconColor(int color) {
        setIconColor(0, color);
    }

    public void setDataActivityColor(int index, int color) {
        mDataActivityColor[index] = color;
    }

    public void setDataActivityColor(int color) {
        setDataActivityColor(0, color);
    }

    public Drawable applyColorFilter(int index, Drawable drawable, PorterDuff.Mode mode) {
        if (drawable != null) {
            drawable.setColorFilter(mIconColor[index], mode);
        }
        return drawable;
    }

    public Drawable applyColorFilter(int index, Drawable drawable) {
        return applyColorFilter(index, drawable, PorterDuff.Mode.SRC_IN);
    }

    public Drawable applyColorFilter(Drawable drawable) {
        return applyColorFilter(0, drawable, PorterDuff.Mode.SRC_IN);
    }

    public Drawable applyColorFilter(Drawable drawable, PorterDuff.Mode mode) {
        return applyColorFilter(0, drawable, mode);
    }

    public Drawable applyDataActivityColorFilter(int index, Drawable drawable) {
        drawable.setColorFilter(mDataActivityColor[index], PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    public Drawable applyDataActivityColorFilter(Drawable drawable) {
        return applyDataActivityColorFilter(0, drawable);
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

    public Drawable getMobileIcon(int index, int resId) {
        Drawable cd;
        String key;

        try {
            key = mSystemUiRes.getResourceEntryName(resId);
        } catch (Resources.NotFoundException nfe) {
            return null;
        }

        mAllowMobileIconChange[index] = !Utils.isMtkDevice();
        mAllowMobileIconChange[index] |= (index == 0) ? 
                key.contains("blue") : key.contains("orange");
        if (!mAllowMobileIconChange[index]) {
            return null;
        }

        switch(mSignalIconMode) {
            case SI_MODE_GB:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                if (mMobileIconIds.containsKey(key)) {
                    Drawable d = mResources.getDrawable(mMobileIconIds.get(key)).mutate();
                    d = applyColorFilter(index, d);
                    setCachedDrawable(key, d);
                    return d;
                }
                if (DEBUG) log("getMobileIcon: no drawable for key: " + key);
                return null;

            case SI_MODE_STOCK:
                cd = getCachedDrawable(key);
                if (cd != null) return cd;
                Drawable d = mSystemUiRes.getDrawable(resId).mutate();
                d = applyColorFilter(index, d);
                setCachedDrawable(key, d);
                return d;

            case SI_MODE_DISABLED:
            default:
                return null;
        }
    }

    public Drawable getMobileIcon(int resId) {
        return getMobileIcon(0, resId);
    }

    public boolean isMobileIconChangeAllowed(int index) {
        return mAllowMobileIconChange[index];
    }

    public boolean isMobileIconChangeAllowed() {
        return isMobileIconChangeAllowed(0);
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

    public Drawable getBasicIcon(int resId) {
        if (resId == 0) return null;

        try {
            String key = mSystemUiRes.getResourceEntryName(resId);
            Drawable d = getCachedDrawable(key);
            if (d != null) return d;
            if (mBasicIconIds.containsKey(key)) {
                if (mBasicIconIds.get(key) != null) {
                    d = mResources.getDrawable(mBasicIconIds.get(key)).mutate();
                    d = applyColorFilter(d);
                } else {
                    d = mSystemUiRes.getDrawable(resId).mutate();
                    d = applyColorFilter(d, PorterDuff.Mode.SRC_ATOP);
                }
                setCachedDrawable(key, d);
                if (DEBUG) log("getBasicIcon: returning drawable for key: " + key);
                return d;
            }
            if (DEBUG) log("getBasicIcon: no record for key: " + key);
            return null;
        } catch (Throwable t) {
            log("getBasicIcon: " + t.getMessage());
            return null;
        }
    }
}
