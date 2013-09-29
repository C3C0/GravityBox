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

import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixTtsSettings {
    private static final String TAG = "GB:FixTtsSettings";
    public static final String PACKAGE_NAME = "com.android.settings";
    private static final String CLASS_VOICEIO_SETTINGS = "com.android.settings.VoiceInputOutputSettings";
    private static final boolean DEBUG = false;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {

            final Class<?> classVoiceIoSettings = XposedHelpers.findClass(CLASS_VOICEIO_SETTINGS, classLoader);

            if (DEBUG) log("replacing populateOrRemovePreferences method");
            XposedHelpers.findAndHookMethod(classVoiceIoSettings, "populateOrRemovePreferences", 
                    new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            boolean hasRecognizer = (Boolean) XposedHelpers.callMethod(param.thisObject, 
                                            "populateOrRemoveRecognizerPrefs");
                            boolean hasTts = (Boolean) XposedHelpers.callMethod(param.thisObject,
                                            "populateOrRemoveTtsPrefs");
                            if (DEBUG) log("populateOrRemovePreferences: hasRecognizer=" + hasRecognizer + "; hasTts=" + hasTts);

                            if (hasRecognizer || hasTts) {
                                return null;
                            }

                            PreferenceFragment fragment = (PreferenceFragment) XposedHelpers.getObjectField(
                                    param.thisObject, "mFragment");
                            PreferenceCategory prefCat = (PreferenceCategory) XposedHelpers.getObjectField(
                                    param.thisObject, "mVoiceCategory");
                            if (fragment != null && prefCat != null) {
                                fragment.getPreferenceScreen().removePreference(prefCat);
                            }

                            return null;
                        }
                
            });
        }
        catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}