package com.ceco.gm2.gravitybox;

import de.robv.android.xposed.XposedBridge;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class Utils {

    // Device types
    private static final int DEVICE_PHONE = 0;
    private static final int DEVICE_HYBRID = 1;
    private static final int DEVICE_TABLET = 2;

    // Device type reference
    private static int mDeviceType = -1;
    private static Boolean mHasGeminiSupport = null;
    private static String mDeviceCharacteristics = null;

    private static int getScreenType(Context con) {
        if (mDeviceType == -1) {
            WindowManager wm = (WindowManager)con.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            int shortSize = Math.min(outMetrics.heightPixels, outMetrics.widthPixels);
            int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / outMetrics.densityDpi;
            if (shortSizeDp < 600) {
                // 0-599dp: "phone" UI with a separate status & navigation bar
                mDeviceType =  DEVICE_PHONE;
            } else if (shortSizeDp < 720) {
                // 600-719dp: "phone" UI with modifications for larger screens
                mDeviceType = DEVICE_HYBRID;
            } else {
                // 720dp: "tablet" UI with a single combined status & navigation bar
                mDeviceType = DEVICE_TABLET;
            }
        }
        return mDeviceType;
    }

    public static boolean isPhoneUI(Context con) {
        return getScreenType(con) == DEVICE_PHONE;
    }

    public static boolean isHybridUI(Context con) {
        return getScreenType(con) == DEVICE_HYBRID;
    }

    public static boolean isTabletUI(Context con) {
        return getScreenType(con) == DEVICE_TABLET;
    }

    public static enum MethodState {
        UNKNOWN,
        METHOD_ENTERED,
        METHOD_EXITED
    }

    public static boolean isMtkDevice() {
        return (Build.HARDWARE.toLowerCase().contains("mt6575")
                || Build.HARDWARE.toLowerCase().contains("mt6577")
                || Build.HARDWARE.toLowerCase().contains("mt6589") 
                || Build.HARDWARE.toLowerCase().contains("mt8389"));
    }

    public static boolean hasGeminiSupport() {
        if (mHasGeminiSupport != null) return mHasGeminiSupport;

        mHasGeminiSupport = SystemProp.getBoolean("ro.mediatek.gemini_support", false);
        return mHasGeminiSupport;
    }

    public static String getDeviceCharacteristics() {
        if (mDeviceCharacteristics != null) return mDeviceCharacteristics;

        mDeviceCharacteristics = SystemProp.get("ro.build.characteristics");
        return mDeviceCharacteristics;
    }
    
    static class SystemProp extends Utils {
        
        private SystemProp() {

        }

        // Get the value for the given key
        // @param key: key to lookup
        // @return null if the key isn't found
        public static String get(String key) {
            String ret;

            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (String) callStaticMethod(classSystemProperties, "get", key);
            } catch (Throwable t) {
                XposedBridge.log("Utils: SystemProp.get failed: " + t.getMessage());
                ret = null;
            }
            return ret;
        }

        // Get the value for the given key
        // @param key: key to lookup
        // @param def: default value to return
        // @return if the key isn't found, return def if it isn't null, or an empty string otherwise
        public static String get(String key, String def) {
            String ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (String) callStaticMethod(classSystemProperties, "get", key, def);
            } catch (Throwable t) {
                XposedBridge.log("Utils: SystemProp.get failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Get the value for the given key, and return as an integer
        // @param key: key to lookup
        // @param def: default value to return
        // @return the key parsed as an integer, or def if the key isn't found or cannot be parsed
        public static Integer getInt(String key, Integer def) {
            Integer ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (Integer) callStaticMethod(classSystemProperties, "getInt", key, def);
            } catch (Throwable t) {
                XposedBridge.log("Utils: SystemProp.getInt failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Get the value for the given key, and return as a long
        // @param key: key to lookup
        // @param def: default value to return
        // @return the key parsed as a long, or def if the key isn't found or cannot be parsed
        public static Long getLong(String key, Long def) {
            Long ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (Long) callStaticMethod(classSystemProperties, "getLong", key, def);
            } catch (Throwable t) {
                XposedBridge.log("Utils: SystemProp.getLong failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Get the value (case insensitive) for the given key, returned as a boolean
        // Values 'n', 'no', '0', 'false' or 'off' are considered false
        // Values 'y', 'yes', '1', 'true' or 'on' are considered true
        // If the key does not exist, or has any other value, then the default result is returned
        // @param key: key to lookup
        // @param def: default value to return
        // @return the key parsed as a boolean, or def if the key isn't found or cannot be parsed
        public static Boolean getBoolean(String key, boolean def) {
            Boolean ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (Boolean) callStaticMethod(classSystemProperties, "getBoolean", key, def);
            } catch (Throwable t) {
                XposedBridge.log("Utils: SystemProp.getBoolean failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }
    }
}
