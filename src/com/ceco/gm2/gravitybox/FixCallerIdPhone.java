package com.ceco.gm2.gravitybox;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixCallerIdPhone {
    public static final String TAG = "GB:FixCallerIdPhone";
    public static final String CLASS_PHONE_NUMBER_UTILS = "android.telephony.PhoneNumberUtils";
    private static final boolean DEBUG = false;

    public static void initZygote(final XSharedPreferences prefs) {
        if (DEBUG) XposedBridge.log(TAG + ": initZygote");

        try {
            final Class<?> numUtilsClass = XposedHelpers.findClass(CLASS_PHONE_NUMBER_UTILS, null);

            if (DEBUG) XposedBridge.log(TAG + ": replacing compareLoosely method");
            XposedHelpers.findAndHookMethod(numUtilsClass, "compareLoosely", String.class, String.class, 
                    new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    Object retVal = (Object) PhoneNumberUtils.compareLoosely((String)param.args[0], (String)param.args[1]);
                    if (DEBUG) {
                        XposedBridge.log(TAG + ": invoked replacement compareLoosely(" + 
                                param.args[0] + ", " + param.args[1] + "); retVal = " + retVal.toString());
                    }
                    return retVal;
                }
            });

            if (DEBUG) XposedBridge.log(TAG + ": hooking internalGetStrippedReversed method");
            XposedHelpers.findAndHookMethod(numUtilsClass, "internalGetStrippedReversed", String.class, int.class,
                    new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (DEBUG) {
                        XposedBridge.log(TAG + ": internalGetStrippedReversed: original MIN_MATCH=" +
                                param.args[1] + ", new MIN_MATCH=" + PhoneNumberUtils.MIN_MATCH);
                    }
                    param.args[1] = PhoneNumberUtils.MIN_MATCH;
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}