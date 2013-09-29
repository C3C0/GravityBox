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