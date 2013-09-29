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

import android.animation.ObjectAnimator;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModElectronBeam {
    private static final String TAG = "GB:ModElectronBeam";
    private static final String CLASS_DISPLAY_POWER_STATE = "com.android.server.power.DisplayPowerState";
    private static final String CLASS_DISPLAY_POWER_CONTROLLER = "com.android.server.power.DisplayPowerController";

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> clsDisplayPowerState = XposedHelpers.findClass(CLASS_DISPLAY_POWER_STATE, null);
            final Class<?> clsDisplaPowerController = XposedHelpers.findClass(CLASS_DISPLAY_POWER_CONTROLLER, null);

            XposedHelpers.findAndHookMethod(clsDisplayPowerState, "prepareElectronBeam", int.class, 
                    new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    int ebMode = 
                            prefs.getBoolean(GravityBoxSettings.PREF_KEY_CRT_OFF_EFFECT, false) ? 1 : 2;
                    param.args[0] = ebMode;
                }
                
            });

            XposedHelpers.findAndHookMethod(clsDisplaPowerController, "initialize", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    ObjectAnimator oa = (ObjectAnimator) 
                            XposedHelpers.getObjectField(param.thisObject, "mElectronBeamOffAnimator");
                    if (oa != null) {
                        oa.setDuration(400);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}