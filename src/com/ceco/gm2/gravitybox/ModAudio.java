package com.ceco.gm2.gravitybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XResources;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModAudio {
    private static final String TAG = "ModAudio";
    private static final String CLASS_REMOTE_PLAYBACK_STATE = "android.media.AudioService$RemotePlaybackState";
    private static final String CLASS_VOLUME_STREAM_STATE = "android.media.AudioService$VolumeStreamState";
    private static final String CLASS_AUDIO_SYSTEM = "android.media.AudioSystem";
    private static final String CLASS_AUDIO_SERVICE = "android.media.AudioService";
    private static final int STREAM_MUSIC = 3;
    private static final int VOLUME_STEPS = 30; 

    private static boolean mSafeMediaVolumeEnabled;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED)) {
                mSafeMediaVolumeEnabled = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_SAFE_MEDIA_VOLUME_ENABLED, false);
                log("Safe headset media volume set to: " + mSafeMediaVolumeEnabled);
            }
        }
    };

    public static void initZygote(final XSharedPreferences prefs) {

        XResources.setSystemWideReplacement("android", "bool", "config_safe_media_volume_enabled", true);

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_MUSIC_VOLUME_STEPS, false)) {
            initMusicStream();
        }

        final Class<?> classAudioService = XposedHelpers.findClass(CLASS_AUDIO_SERVICE, null);
        mSafeMediaVolumeEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_SAFE_MEDIA_VOLUME, false);
        log("Safe headset media volume set to: " + mSafeMediaVolumeEnabled);

        XposedBridge.hookAllConstructors(classAudioService, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                if (context == null) return;

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(GravityBoxSettings.ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED);
                context.registerReceiver(mBroadcastReceiver, intentFilter);
                log("AudioService constructed. Broadcast receiver registered");
            }
        });

        XposedHelpers.findAndHookMethod(classAudioService, "enforceSafeMediaVolume", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!mSafeMediaVolumeEnabled) {
                    param.setResult(null);
                    return;
                }
            }
        });

        XposedHelpers.findAndHookMethod(classAudioService, "checkSafeMediaVolume", 
                int.class, int.class, int.class, new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!mSafeMediaVolumeEnabled) {
                    param.setResult(true);
                    return;
                }
            }
        });
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