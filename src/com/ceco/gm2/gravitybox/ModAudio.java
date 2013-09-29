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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XResources;
import android.os.Build;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModAudio {
    private static final String TAG = "GB:ModAudio";
    private static final String CLASS_REMOTE_PLAYBACK_STATE = "android.media.AudioService$RemotePlaybackState";
    private static final String CLASS_VOLUME_STREAM_STATE = "android.media.AudioService$VolumeStreamState";
    private static final String CLASS_AUDIO_SYSTEM = "android.media.AudioSystem";
    private static final String CLASS_AUDIO_SERVICE = "android.media.AudioService";
    private static final int STREAM_MUSIC = 3;
    private static final int VOLUME_STEPS = 30;
    private static final boolean DEBUG = false;

    private static boolean mSafeMediaVolumeEnabled;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED)) {
                mSafeMediaVolumeEnabled = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_SAFE_MEDIA_VOLUME_ENABLED, false);
                if (DEBUG) log("Safe headset media volume set to: " + mSafeMediaVolumeEnabled);
            }
        }
    };

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            final Class<?> classAudioService = XposedHelpers.findClass(CLASS_AUDIO_SERVICE, null);

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_MUSIC_VOLUME_STEPS, false)
                    && Utils.shouldAllowMoreVolumeSteps()) {
                initMusicStream();
            }

            XposedBridge.hookAllConstructors(classAudioService, new XC_MethodHook() {
    
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (context == null) return;
    
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED);
                    context.registerReceiver(mBroadcastReceiver, intentFilter);
                    if (DEBUG) log("AudioService constructed. Broadcast receiver registered");
                }
            });

            if (Build.VERSION.SDK_INT > 16) {
                XResources.setSystemWideReplacement("android", "bool", "config_safe_media_volume_enabled", true);
                mSafeMediaVolumeEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_SAFE_MEDIA_VOLUME, false);
                if (DEBUG) log("Safe headset media volume set to: " + mSafeMediaVolumeEnabled);
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
        } catch(Throwable t) {
            XposedBridge.log(t);
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
                    if (DEBUG) log("RemotePlaybackState constructed. Music stream volume steps set to " + VOLUME_STEPS);
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
                        if (DEBUG) log("Volume for music stream initialized with steps set to " + VOLUME_STEPS);
                    }
                }
            });

        } catch(Throwable t) {
            XposedBridge.log(t);
        }
    }
}