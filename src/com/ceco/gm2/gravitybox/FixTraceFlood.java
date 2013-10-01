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

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixTraceFlood {
    private static final String TAG = "GB:FixTraceFlood";
    private static final String CLASS_TRACE = "android.os.Trace";
    private static final boolean DEBUG = false;

    public static void initZygote() {
        if (DEBUG) XposedBridge.log(TAG + ": initZygote");

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
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}