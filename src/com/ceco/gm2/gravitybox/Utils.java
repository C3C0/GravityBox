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

    public static boolean isPhone(Context con) {
        return getScreenType(con) == DEVICE_PHONE;
    }

    public static boolean isHybrid(Context con) {
        return getScreenType(con) == DEVICE_HYBRID;
    }

    public static boolean isTablet(Context con) {
        return getScreenType(con) == DEVICE_TABLET;
    }

    public static enum MethodState {
        UNKNOWN,
        METHOD_ENTERED,
        METHOD_EXITED
    }

    public static boolean isMtkDevice() {
        return (Build.HARDWARE.toLowerCase().contains("mt6589") || Build.HARDWARE.toLowerCase().contains("mt8389"));
    }

    public static boolean hasGeminiSupport() {
        if (mHasGeminiSupport != null) return mHasGeminiSupport;

        try {
            Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
            String geminiSupport = (String) callStaticMethod(classSystemProperties, 
                    "get", "ro.mediatek.gemini_support");
            mHasGeminiSupport = "true".equals(geminiSupport);
            return mHasGeminiSupport;
        } catch (Throwable t) {
            XposedBridge.log("Utils: hasGeminiSupport check failed. Assuming device has no Gemini support");
            return false;
        }
    }
}
