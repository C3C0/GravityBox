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

import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModSettings {
    private static final String TAG = "GB:ModSettings";
    public static final String PACKAGE_NAME = "com.android.settings";
    private static final String CLASS_PU_SUMMARY = "com.android.settings.fuelgauge.PowerUsageSummary";
    private static final boolean DEBUG = false;

    private static void log (String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classPowerUsageSummary = XposedHelpers.findClass(CLASS_PU_SUMMARY, classLoader);

            XposedHelpers.findAndHookMethod(classPowerUsageSummary, "refreshStats", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    PreferenceGroup appListGroup = (PreferenceGroup) XposedHelpers.getObjectField(
                            param.thisObject, "mAppListGroup");
                    if (appListGroup == null) return;

                    int prefCount = appListGroup.getPreferenceCount();
                    for (int i = 0; i < prefCount; i++) {
                        // We assume there's only one checkbox preference which is the battery percent checkbox
                        // Assumption is a mother of all fuckups, thus:
                        // TODO: make this more bullet-proof
                        Preference pref = appListGroup.getPreference(i);
                        if (pref != null && pref instanceof CheckBoxPreference) {
                            if (DEBUG) log("CheckBoxPreference found: " + pref.getTitle() + " - removing");
                            appListGroup.removePreference(pref);
                            break;
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}