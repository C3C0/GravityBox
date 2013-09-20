package com.ceco.gm2.gravitybox;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public class StatusBarIconManager {
    private static final String TAG = "StatusBarIconStore";
    private static final boolean DEBUG = false;
    public static final int DEFAULT_DATA_ACTIVITY_COLOR = Color.WHITE;

    private Resources mResources;
    private int mIconColor;
    private int mDataActivityColor;
    private Map<String, Integer> mWifiIconIds;
    private Map<String, Integer> mMobileIconIds;
    private Map<String, Integer> mBatteryIconIds;
    private Map<String, SoftReference<Drawable>> mIconCache;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public StatusBarIconManager(Resources res) {
        mResources = res;
        mIconColor = getDefaultIconColor();
        mDataActivityColor = DEFAULT_DATA_ACTIVITY_COLOR;

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
        return mResources.getColor(android.R.color.holo_blue_dark);
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

    public Drawable getWifiIcon(String key) {
        Drawable cd = getCachedDrawable(key);
        if (cd != null) return cd;

        if (mWifiIconIds.containsKey(key)) {
            Drawable d = mResources.getDrawable(mWifiIconIds.get(key)).mutate();
            d = applyColorFilter(d);
            setCachedDrawable(key, d);
            return d;
        }

        if (DEBUG) log("getWifiIcon: no drawable for key: " + key);
        return null;
    }

    public Drawable getMobileIcon(String key) {
        Drawable cd = getCachedDrawable(key);
        if (cd != null) return cd;

        if (mMobileIconIds.containsKey(key)) {
            Drawable d = mResources.getDrawable(mMobileIconIds.get(key)).mutate();
            d = applyColorFilter(d);
            setCachedDrawable(key, d);
            return d;
        }

        if (DEBUG) log("getMobileIcon: no drawable for key: " + key);
        return null;
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
