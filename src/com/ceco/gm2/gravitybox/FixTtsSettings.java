package com.ceco.gm2.gravitybox;

import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FixTtsSettings {
    private static final String TAG = "FixTtsSettings";
    public static final String PACKAGE_NAME = "com.android.settings";
    private static final String CLASS_VOICEIO_SETTINGS = "com.android.settings.VoiceInputOutputSettings";

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {

            final Class<?> classVoiceIoSettings = XposedHelpers.findClass(CLASS_VOICEIO_SETTINGS, classLoader);

            log("replacing populateOrRemovePreferences method");
            XposedHelpers.findAndHookMethod(classVoiceIoSettings, "populateOrRemovePreferences", 
                    new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            boolean hasRecognizer = (Boolean) XposedHelpers.callMethod(param.thisObject, 
                                            "populateOrRemoveRecognizerPrefs");
                            boolean hasTts = (Boolean) XposedHelpers.callMethod(param.thisObject,
                                            "populateOrRemoveTtsPrefs");
                            log("populateOrRemovePreferences: hasRecognizer=" + hasRecognizer + "; hasTts=" + hasTts);

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
        catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}