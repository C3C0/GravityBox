package com.ceco.gm2.gravitybox;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModAudio {
    private static final String TAG = "ModAudio";
    private static final String CLASS_REMOTE_PLAYBACK_STATE = "android.media.AudioService$RemotePlaybackState";
    private static final String CLASS_VOLUME_STREAM_STATE = "android.media.AudioService$VolumeStreamState";
    private static final String CLASS_AUDIO_SYSTEM = "android.media.AudioSystem";
    private static final int STREAM_MUSIC = 3;
    private static final int VOLUME_STEPS = 30; 

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initZygote(final XSharedPreferences prefs) {

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_MUSIC_VOLUME_STEPS, false)) {
            initMusicStream();
        }
    }

    private static void initMusicStream() {
        try {
            final Class<?> classRemotePlaybackState = XposedHelpers.findClass(
                    CLASS_REMOTE_PLAYBACK_STATE, null);
            final Class<?> classVolumeStreamState = XposedHelpers.findClass(
                    CLASS_VOLUME_STREAM_STATE, null);
            final Class<?> classAudioSystem = XposedHelpers.findClass(CLASS_AUDIO_SYSTEM, null);

            XposedBridge.hookAllConstructors(classRemotePlaybackState, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[1] = VOLUME_STEPS;
                    param.args[2] = VOLUME_STEPS;
                    log("RemotePlaybackState constructed. Music stream volume steps set to " + VOLUME_STEPS);
                }
            });

            XposedBridge.hookAllConstructors(classVolumeStreamState, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int streamType = XposedHelpers.getIntField(param.thisObject, "mStreamType");
                    if (streamType == STREAM_MUSIC) {
                        XposedHelpers.setIntField(param.thisObject, "mIndexMax", (VOLUME_STEPS*10));
                        XposedHelpers.callStaticMethod(
                                classAudioSystem, "initStreamVolume", STREAM_MUSIC, 0, VOLUME_STEPS);
                        XposedHelpers.callMethod(param.thisObject, "readSettings");
                        log("Volume for music stream initialized with steps set to " + VOLUME_STEPS);
                    }
                }
            });

        } catch(Exception e) {
            XposedBridge.log(e);
        }
    }
}