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

import android.app.Activity;
import android.content.Intent;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModCellConnService {
    public static final String PACKAGE_NAME = "com.mediatek.CellConnService";
    private static final String TAG = "GB:ModCellConnService";
    private static final String CLASS_CONFIRM_DLG = "com.mediatek.CellConnService.ConfirmDlgActivity";
    private static final boolean DEBUG = false;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classConfirmDlg = XposedHelpers.findClass(CLASS_CONFIRM_DLG, classLoader);

            XposedHelpers.findAndHookMethod(classConfirmDlg, "onNewIntent", Intent.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    Intent intent = (Intent) param.args[0];
                    if (DEBUG) log("onNewIntent: " + ((intent == null) ? "NULL" : intent.toString()));

                    if (intent == null
                            || !prefs.getBoolean(GravityBoxSettings.PREF_KEY_ROAMING_WARNING_DISABLE, false)) {
                        return;
                    }

                    if (intent.getIntExtra("confirm_type", 0) == 0x195
                            && !intent.getBooleanExtra("confirm_roamingWithPrefer", false)) {
                        if (DEBUG) log("Roaming warning intent received - ignoring");
                        XposedHelpers.callMethod(param.thisObject, "sendConfirmResult", 0x195, true);
                        ((Activity)param.thisObject).finish();
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}