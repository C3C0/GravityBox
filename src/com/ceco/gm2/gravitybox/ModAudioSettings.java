package com.ceco.gm2.gravitybox;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.Unhook;

public class ModAudioSettings {
    private static final String TAG = "ModAudioSettings";
    public static final String PACKAGE_NAME = "com.android.settings";
    private static final String CLASS_VOLUME_PREF = "com.mediatek.audioprofile.RingerVolumePreference";
    private static final String CLASS_VOLUMIZER = "com.mediatek.audioprofile.RingerVolumePreference$SeekBarVolumizer";
    private static final boolean DEBUG = false;

    private static Unhook mSetVolumeHook = null;
    private static Unhook mRevertVolumeHook = null;
    private static Unhook mSaveVolumeHook = null;

    private static void log (String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classVolumePref = XposedHelpers.findClass(CLASS_VOLUME_PREF, classLoader);
            final Class<?> classVolumizer = XposedHelpers.findClass(CLASS_VOLUMIZER, classLoader);

            XposedHelpers.findAndHookMethod(classVolumePref, "onBindDialogView", View.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true)) return;

                    Object[] mSeekBarVolumizer = (Object[]) XposedHelpers.getObjectField(
                            param.thisObject, "mSeekBarVolumizer");
                    XposedHelpers.callMethod(mSeekBarVolumizer[0], "setVisible", true);

                    View v = (View) param.args[0];
                    Resources res = v.getContext().getResources();
                    int resId = res.getIdentifier(
                            "notification_section", "id", PACKAGE_NAME);
                    View notifView = ((View) param.args[0]).findViewById(resId);
                    if (notifView != null) notifView.setVisibility(View.VISIBLE);

                    int ringerTitleId = res.getIdentifier("ring_volume_title", "string", PACKAGE_NAME);
                    if (ringerTitleId != 0) {
                        int rvTextId = res.getIdentifier("ringer_description_text", "id", PACKAGE_NAME);
                        TextView rvText = (TextView) ((View) param.args[0]).findViewById(rvTextId);
                        if (rvText != null) {
                            rvText.setText(ringerTitleId);
                        }
                    }

                    int[] streamType = (int[]) XposedHelpers.getStaticObjectField(
                            param.thisObject.getClass(), "STREAM_TYPE");
                    streamType[0] = 5;
                    streamType[1] = 2; 
                }
            });

            XposedHelpers.findAndHookMethod(classVolumizer, 
                    "setVolume", int.class, int.class, boolean.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true)) return;

                    if (DEBUG ) log("SeekBarVolumizer setVolume: streamType=" + param.args[0] +
                            "; volume=" + param.args[1] + "; flag=" + param.args[2]);

                    param.args[2] = true;
                    final int streamType = (Integer) param.args[0];
                    final Object audioManager = XposedHelpers.getObjectField(
                            XposedHelpers.getSurroundingThis(param.thisObject), "mAudioManager");

                    mSetVolumeHook = XposedHelpers.findAndHookMethod(audioManager.getClass(), 
                            "setAudioProfileStreamVolume", int.class, int.class, int.class, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param2) throws Throwable {
                            if (DEBUG) log ("mAudioManager setAudioProfileStreamVolume: " + 
                                    param2.args[0] + "," + param2.args[1] + "," + param2.args[2]);
                            if ((Integer)param2.args[0] != streamType) {
                                log("setVolume: mAudioManager.setAudioProfileStreamVolume: " +
                                		"Attempt to set volume of foreign Stream Type - ignoring");
                                param2.args[0] = streamType;
                            }
                        }

                    });
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mSetVolumeHook != null) {
                        mSetVolumeHook.unhook();
                        mSetVolumeHook = null;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classVolumizer, "revertVolume", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true)) return;

                    final int streamType = XposedHelpers.getIntField(param.thisObject, "mStreamType");
                    final Object profileManager = XposedHelpers.getObjectField(
                            XposedHelpers.getSurroundingThis(param.thisObject), "mProfileManager");

                    mRevertVolumeHook = XposedHelpers.findAndHookMethod(
                            profileManager.getClass(), "setStreamVolume", 
                            String.class, int.class, int.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param2) throws Throwable {
                                    if ((Integer) param2.args[1] != streamType) {
                                        log("revertVolume: setStreamVolume: " +
                                        		"Attempt to set volume of foreign Stream Type - ignoring");
                                        param2.args[1] = streamType;
                                    }
                                }
                            });
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mRevertVolumeHook != null) {
                        mRevertVolumeHook.unhook();
                        mRevertVolumeHook = null;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classVolumizer, "saveVolume", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true)) return;

                    final int streamType = XposedHelpers.getIntField(param.thisObject, "mStreamType");
                    final Object profileManager = XposedHelpers.getObjectField(
                            XposedHelpers.getSurroundingThis(param.thisObject), "mProfileManager");

                    mSaveVolumeHook = XposedHelpers.findAndHookMethod(
                            profileManager.getClass(), "setStreamVolume", 
                            String.class, int.class, int.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param2) throws Throwable {
                                    if ((Integer) param2.args[1] != streamType) {
                                        log("saveVolume: setStreamVolume: " +
                                        		"Attempt to set volume of foreign Stream Type - ignoring");
                                        param2.args[1] = streamType;
                                    }
                                }
                            });
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mSaveVolumeHook != null) {
                        mSaveVolumeHook.unhook();
                        mSaveVolumeHook = null;
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}