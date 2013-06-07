package com.ceco.gm2.gravitybox;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixTraceFlood {
    private static final String TAG = "FixTraceFlood";
    private static final String CLASS_TRACE = "android.os.Trace";

    public static void initZygote() {
        XposedBridge.log(TAG + ": initZygote");

        try {
            final Class<?> traceClass = XposedHelpers.findClass(CLASS_TRACE, null);

            XposedHelpers.findAndHookMethod(traceClass, "cacheEnabledTags", new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    long tags = (Long) XposedHelpers.callStaticMethod(traceClass, "nativeGetEnabledTags");
                    XposedHelpers.setStaticLongField(traceClass, "sEnabledTags", tags);
                    return tags;
                }
                
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}