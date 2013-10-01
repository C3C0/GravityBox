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

import java.util.Date;
import java.util.TimeZone;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixDateTimeCrash {
    private static final String TAG = "GB:FixDateTimeCrash";
    public static final String PACKAGE_NAME = "com.android.settings";
    public static final String CLASS_DATETIME_SETTINGS = "com.android.settings.DateTimeSettings";
    private static final boolean DEBUG = false;

    public static void init (final XSharedPreferences prefs, final ClassLoader classLoader) {
        if (DEBUG) XposedBridge.log(TAG + ": init");

        try {
            Class<?> dtSettingsClass = XposedHelpers.findClass(CLASS_DATETIME_SETTINGS, classLoader);

            XposedHelpers.findAndHookMethod(dtSettingsClass, "getTimeZoneText", TimeZone.class,
                    new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            // Similar to new SimpleDateFormat("'GMT'Z, zzzz").format(new Date()), but
                            // we want "GMT-03:00" rather than "GMT-0300".
                            if (DEBUG) XposedBridge.log(TAG + ": running replaced getTimeZoneText() method");
                            TimeZone tz = (TimeZone) param.args[0];
                            Date now = new Date();
                            return formatOffset(new StringBuilder(), tz, now).
                                    append(", ").
                                    append(tz.getDisplayName(tz.inDaylightTime(now), TimeZone.LONG)).toString();
                        }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static StringBuilder formatOffset(StringBuilder sb, TimeZone tz, Date d) {
        int off = tz.getOffset(d.getTime()) / 1000 / 60;

        sb.append("GMT");
        if (off < 0) {
            sb.append('-');
            off = -off;
        } else {
            sb.append('+');
        }

        int hours = off / 60;
        int minutes = off % 60;

        sb.append((char) ('0' + hours / 10));
        sb.append((char) ('0' + hours % 10));

        sb.append(':');

        sb.append((char) ('0' + minutes / 10));
        sb.append((char) ('0' + minutes % 10));

        return sb;
    }
}
