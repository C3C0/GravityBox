package com.ceco.gm2.gravitybox;

import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    private static int STREAM_RING = 2;
    private static int STREAM_NOTIFICATION = 5;

    private static Object mVolumePanel;
    private static Object mAudioService;
    private static boolean mVolumesLinked;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBrodcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_VOLUME_PANEL_MODE_CHANGED)) {
                boolean expandable = intent.getBooleanExtra(GravityBoxSettings.EXTRA_EXPANDABLE, false);
                updateVolumePanelMode(expandable);
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_LINK_VOLUMES_CHANGED)) {
                mVolumesLinked = intent.getBooleanExtra(GravityBoxSettings.EXTRA_LINKED, true);
                log("mVolumesLinked set to: " + mVolumesLinked);
                updateStreamVolumeAlias();
            }
        }
        
    };

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classVolumePanel = XposedHelpers.findClass(CLASS_VOLUME_PANEL, classLoader);
            final Class<?> classAudioService = XposedHelpers.findClass(CLASS_AUDIO_SERVICE, classLoader);

            XposedBridge.hookAllConstructors(classVolumePanel, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mVolumePanel = param.thisObject;
                    Context context = (Context) XposedHelpers.getObjectField(mVolumePanel, "mContext");
                    log("VolumePanel constructed; mVolumePanel set");

                    boolean expandable = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_VOLUME_PANEL_EXPANDABLE, true);
                    updateVolumePanelMode(expandable);

                    mVolumesLinked = prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true);

                    Object[] streams = (Object[]) XposedHelpers.getStaticObjectField(classVolumePanel, "STREAMS");
                    XposedHelpers.setBooleanField(streams[1], "show", true);

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_LINK_VOLUMES_CHANGED);
                    context.registerReceiver(mBrodcastReceiver, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(classVolumePanel, "expand", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    hideNotificationSliderIfLinked();
                }
            });

            XposedBridge.hookAllConstructors(classAudioService, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mAudioService = param.thisObject;
                    mVolumesLinked = prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true);
                    log("AudioService constructed: mAudioService set");
                    updateStreamVolumeAlias();
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void updateVolumePanelMode(boolean expandable) {
        if (mVolumePanel == null) return;

        View mMoreButton = (View) XposedHelpers.getObjectField(mVolumePanel, "mMoreButton");
        View mDivider = (View) XposedHelpers.getObjectField(mVolumePanel, "mDivider");

        mMoreButton.setVisibility(expandable ? View.VISIBLE : View.GONE);
        if (!mMoreButton.hasOnClickListeners()) {
            mMoreButton.setOnClickListener((OnClickListener) mVolumePanel);
        }
        mDivider.setVisibility(expandable ? View.VISIBLE : View.GONE);

        XposedHelpers.setBooleanField(mVolumePanel, "mShowCombinedVolumes", expandable);
        XposedHelpers.setBooleanField(mVolumePanel, "mVoiceCapable", false);
        XposedHelpers.setObjectField(mVolumePanel, "mStreamControls", null);
        log("VolumePanel mode changed to: " + ((expandable) ? "EXPANDABLE" : "SIMPLE"));
    }

    private static void hideNotificationSliderIfLinked() {
        if (mVolumePanel == null || !mVolumesLinked) return;

        @SuppressWarnings("unchecked")
        Map<Integer, Object> streamControls = 
                (Map<Integer, Object>) XposedHelpers.getObjectField(mVolumePanel, "mStreamControls");
        if (streamControls == null) return;

        for (Object o : streamControls.values()) {
            if ((Integer) XposedHelpers.getIntField(o, "streamType") == STREAM_NOTIFICATION) {
                View v = (View) XposedHelpers.getObjectField(o, "group");
                if (v != null) {
                    v.setVisibility(View.GONE);
                    log("Notification volume slider hidden");
                    break;
                }
            }
        }
    }

    private static void updateStreamVolumeAlias() {
        if (mAudioService == null) return;

        int[] streamVolumeAlias = (int[]) XposedHelpers.getObjectField(mAudioService, "mStreamVolumeAlias");
        streamVolumeAlias[STREAM_NOTIFICATION] = mVolumesLinked ? STREAM_RING : STREAM_NOTIFICATION;
        XposedHelpers.setObjectField(mAudioService, "mStreamVolumeAlias", streamVolumeAlias);
        log("AudioService mStreamVolumeAlias updated, STREAM_NOTIFICATION set to: " + 
                streamVolumeAlias[STREAM_NOTIFICATION]);
    }
}