package com.ceco.gm2.gravitybox;

import java.util.Date;
import java.util.TimeZone;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixDateTimeCrash {
    private static final String TAG = "FixDateTimeCrash";
    public static final String PACKAGE_NAME = "com.android.settings";
    public static final String CLASS_DATETIME_SETTINGS = "com.android.settings.DateTimeSettings";

    public static void init (final XSharedPreferences prefs, final ClassLoader classLoader) {
        XposedBridge.log(TAG + ": init");

        try {
            Class<?> dtSettingsClass = XposedHelpers.findClass(CLASS_DATETIME_SETTINGS, classLoader);

            XposedHelpers.findAndHookMethod(dtSettingsClass, "getTimeZoneText", TimeZone.class,
                    new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            // Similar to new SimpleDateFormat("'GMT'Z, zzzz").format(new Date()), but
                            // we want "GMT-03:00" rather than "GMT-0300".
                            XposedBridge.log(TAG + ": running replaced getTimeZoneText() method");
                            TimeZone tz = (TimeZone) param.args[0];
                            Date now = new Date();
                            return formatOffset(new StringBuilder(), tz, now).
                                    append(", ").
                                    append(tz.getDisplayName(tz.inDaylightTime(now), TimeZone.LONG)).toString();
                        }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
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
