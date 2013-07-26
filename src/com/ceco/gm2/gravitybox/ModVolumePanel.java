package com.ceco.gm2.gravitybox;

import android.view.View;
import android.view.View.OnClickListener;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModVolumePanel {
    private static final String TAG = "ModVolumePanel";
    public static final String PACKAGE_NAME = "android";
    private static final String CLASS_VOLUME_PANEL = "android.view.VolumePanel";
    private static final String CLASS_AUDIO_SERVICE = "android.media.AudioService";

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classVolumePanel = XposedHelpers.findClass(CLASS_VOLUME_PANEL, classLoader);
            final Class<?> classAudioService = XposedHelpers.findClass(CLASS_AUDIO_SERVICE, classLoader);

            XposedBridge.hookAllConstructors(classVolumePanel, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    log("VolmePanel after construct hook");
                    View mMoreButton = (View) XposedHelpers.getObjectField(param.thisObject, "mMoreButton");
                    View mDivider = (View) XposedHelpers.getObjectField(param.thisObject, "mDivider");

                    mMoreButton.setVisibility(View.VISIBLE);
                    mMoreButton.setOnClickListener((OnClickListener) param.thisObject);
                    mDivider.setVisibility(View.VISIBLE);

                    XposedHelpers.setBooleanField(param.thisObject, "mShowCombinedVolumes", true);
                    XposedHelpers.setBooleanField(param.thisObject, "mVoiceCapable", false);
                }
            });

            XposedHelpers.findAndHookMethod(classAudioService, 
                    "updateStreamVolumeAlias", boolean.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    int[] streamVolumeAlias = (int[]) XposedHelpers.getObjectField(param.thisObject, "mStreamVolumeAlias");
                    streamVolumeAlias[5] = 5;
                    XposedHelpers.setObjectField(param.thisObject, "mStreamVolumeAlias", streamVolumeAlias);
                    log("AudioService updateStreamVolumeAlias(): STREAM_NOTIFICATION volume alias set");
                }
            });

        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}
