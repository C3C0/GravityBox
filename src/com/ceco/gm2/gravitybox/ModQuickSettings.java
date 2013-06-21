package com.ceco.gm2.gravitybox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ceco.gm2.gravitybox.quicksettings.AQuickSettingsTile;
import com.ceco.gm2.gravitybox.quicksettings.NetworkModeTile;
import com.ceco.gm2.gravitybox.quicksettings.TorchTile;
import com.ceco.gm2.gravitybox.quicksettings.GravityBoxTile;
import com.ceco.gm2.gravitybox.quicksettings.SyncTile;
import com.ceco.gm2.gravitybox.quicksettings.WifiApTile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModQuickSettings {
    private static final String TAG = "ModQuickSettings";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_QUICK_SETTINGS = "com.android.systemui.statusbar.phone.QuickSettings";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_BASE_STATUSBAR = "com.android.systemui.statusbar.BaseStatusBar";
    private static final String CLASS_PANEL_BAR = "com.android.systemui.statusbar.phone.PanelBar";
    private static final String CLASS_QS_TILEVIEW = "com.android.systemui.statusbar.phone.QuickSettingsTileView";
    private static final boolean DEBUG = false;

    private static Context mContext;
    private static Context mGbContext;
    private static ViewGroup mContainerView;
    private static Object mPanelBar;
    private static Object mStatusBar;
    private static Set<String> mActiveTileKeys;
    private static Class<?> mQuickSettingsTileViewClass;

    private static ArrayList<AQuickSettingsTile> mTiles;

    private static List<String> mCustomSystemTileKeys = new ArrayList<String>(Arrays.asList(
            "user_textview",
            "airplane_mode_textview",
            "battery_textview",
            "wifi_textview",
            "bluetooth_textview",
            "gps_textview",
            "data_conn_textview",
            "rssi_textview",
            "audio_profile_textview",
            "brightness_textview",
            "timeout_textview",
            "auto_rotate_textview"
    ));

    private static List<String> mCustomGbTileKeys = new ArrayList<String>(Arrays.asList(
            "sync_tileview",
            "wifi_ap_tileview",
            "gravitybox_tileview",
            "torch_tileview",
            "network_mode_tileview"
    ));

    private enum RemoveNotificationMethodState {
        METHOD_ENTERED,
        METHOD_EXITED
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            log("received broadcast: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_QUICKSETTINGS_CHANGED) &&
                            intent.hasExtra(GravityBoxSettings.EXTRA_QS_PREFS)) {
                String[] qsPrefs = intent.getStringArrayExtra(GravityBoxSettings.EXTRA_QS_PREFS);
                mActiveTileKeys = new HashSet<String>(Arrays.asList(qsPrefs));
                updateTileVisibility();
            }
        }
    };

    // TODO: quickfix that needs some optimizations
    private static boolean isCustomizableTile(View view) {
        Resources res = mContext.getResources();
        for (String key : mCustomSystemTileKeys) {
            int resId = res.getIdentifier(key, "id", PACKAGE_NAME);
            if (view.findViewById(resId) != null) {
                return true;
            }
        }

        res = mGbContext.getResources();
        for (String key : mCustomGbTileKeys) {
            int resId = res.getIdentifier(key, "id", GravityBox.PACKAGE_NAME);
            if (view.findViewById(resId) != null) {
                return true;
            }
        }

        return false;
    }

    private static void updateTileVisibility() {

        if (mActiveTileKeys == null) {
            log("updateTileVisibility: mActiveTileKeys is null - skipping");
            return;
        }

        int tileCount = mContainerView.getChildCount();

        // hide all tiles first
        for(int i = 0; i < tileCount; i++) {
            View view = mContainerView.getChildAt(i);
            if (view != null && isCustomizableTile(view)) {
                view.setVisibility(View.GONE);
            }
        }

        // unhide only those tiles present in mActiveTileKeys set
        for(String tileKey : mActiveTileKeys) {
            // search within mContext resources (system specific tiles)
            View view = mContainerView.findViewById(mContext.getResources().getIdentifier(
                    tileKey, "id", PACKAGE_NAME));
            if (view == null) {
                // search within mGbContext (our additional GB specific tiles)
                view = mContainerView.findViewById(mGbContext.getResources().getIdentifier(
                        tileKey, "id", GravityBox.PACKAGE_NAME));
            }

            if (view != null) {
                if (DEBUG) {
                    log("updateTileVisibility: unhiding tile for key: " + tileKey + "; " +
                            "view=" + ((view == null) ? "null" : view.toString()));
                }

                // bubble up in view hierarchy to find QuickSettingsTileView parent view
                View rootView = view;
                do {
                    rootView = (View) rootView.getParent();
                } while (rootView != null && rootView.getClass() != mQuickSettingsTileViewClass);

                if (DEBUG) {
                    log("updateTileVisibility: finished searching for root view; rootView=" +
                            ((rootView == null) ? "null" : rootView.toString()));
                }

                if (rootView != null) {
                    rootView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        log("init");

        try {
            final ThreadLocal<RemoveNotificationMethodState> removeNotificationState = 
                    new ThreadLocal<RemoveNotificationMethodState>();

            prefs.reload();
            mActiveTileKeys = prefs.getStringSet(GravityBoxSettings.PREF_KEY_QUICK_SETTINGS, null);
            log("got tile prefs: mActiveTileKeys = " + (mActiveTileKeys == null ? "null" : mActiveTileKeys.toString()));

            final Class<?> quickSettingsClass = XposedHelpers.findClass(CLASS_QUICK_SETTINGS, classLoader);
            final Class<?> phoneStatusBarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> panelBarClass = XposedHelpers.findClass(CLASS_PANEL_BAR, classLoader);
            mQuickSettingsTileViewClass = XposedHelpers.findClass(CLASS_QS_TILEVIEW, classLoader);
            final Class<?> baseStatusBarClass = XposedHelpers.findClass(CLASS_BASE_STATUSBAR, classLoader);

            XposedBridge.hookAllConstructors(quickSettingsClass, quickSettingsConstructHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "setBar", 
                    panelBarClass, quickSettingsSetBarHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "setService", 
                    phoneStatusBarClass, quickSettingsSetServiceHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "addSystemTiles", 
                    ViewGroup.class, LayoutInflater.class, quickSettingsAddSystemTilesHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "updateResources", quickSettingsUpdateResourcesHook);

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "removeNotification", IBinder.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) {
                        log("removeNotification method ENTER");
                    }
                    removeNotificationState.set(RemoveNotificationMethodState.METHOD_ENTERED);
                }

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) {
                        log("removeNotification method EXIT");
                    }
                    removeNotificationState.set(RemoveNotificationMethodState.METHOD_EXITED);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "animateCollapsePanels", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (removeNotificationState.get() != null && 
                            removeNotificationState.get().equals(RemoveNotificationMethodState.METHOD_ENTERED)) {
                        log("animateCollapsePanels called from removeNotification method");

                        boolean hasFlipSettings = XposedHelpers.getBooleanField(param.thisObject, "mHasFlipSettings");
                        boolean animating = XposedHelpers.getBooleanField(param.thisObject, "mAnimating");
                        View flipSettingsView = (View) XposedHelpers.getObjectField(param.thisObject, "mFlipSettingsView");
                        Object baseStatusBar = baseStatusBarClass.cast(param.thisObject);
                        Object notificationData = XposedHelpers.getObjectField(baseStatusBar, "mNotificationData");
                        int ndSize = (Integer) XposedHelpers.callMethod(notificationData, "size");
                        boolean isShowingSettings = hasFlipSettings && flipSettingsView.getVisibility() == View.VISIBLE;

                        if (ndSize == 0 && !animating && !isShowingSettings) {
                            // let the original method finish its work
                        } else {
                            log("animateCollapsePanels: all notifications removed but showing QuickSettings - do nothing");
                            param.setResult(null);
                        }
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static XC_MethodHook quickSettingsConstructHook = new XC_MethodHook() {

        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            log("QuickSettings constructed - initializing local members");

            mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            mGbContext = mContext.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            mContainerView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mContainerView");

            IntentFilter intentFilter = new IntentFilter(GravityBoxSettings.ACTION_PREF_QUICKSETTINGS_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    };

    private static XC_MethodHook quickSettingsSetBarHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            mPanelBar = param.args[0];
            log("mPanelBar set");
        }
    };

    private static XC_MethodHook quickSettingsSetServiceHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            mStatusBar = param.args[0];
            log("mStatusBar set");
        }
    };

    private static XC_MethodHook quickSettingsAddSystemTilesHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            log("about to add tiles");

            LayoutInflater inflater = (LayoutInflater) param.args[1];

            mTiles = new ArrayList<AQuickSettingsTile>();

            NetworkModeTile nmTile = new NetworkModeTile(mContext, mGbContext, mStatusBar, mPanelBar);
            nmTile.setupQuickSettingsTile(mContainerView, inflater);
            mTiles.add(nmTile);

            SyncTile syncTile = new SyncTile(mContext, mGbContext, mStatusBar, mPanelBar);
            syncTile.setupQuickSettingsTile(mContainerView, inflater);
            mTiles.add(syncTile);

            WifiApTile wifiApTile = new WifiApTile(mContext, mGbContext, mStatusBar, mPanelBar);
            wifiApTile.setupQuickSettingsTile(mContainerView, inflater);
            mTiles.add(wifiApTile);

            TorchTile torchTile = new TorchTile(mContext, mGbContext, mStatusBar, mPanelBar);
            torchTile.setupQuickSettingsTile(mContainerView, inflater);
            mTiles.add(torchTile);

            GravityBoxTile gbTile = new GravityBoxTile(mContext, mGbContext, mStatusBar, mPanelBar);
            gbTile.setupQuickSettingsTile(mContainerView, inflater);
            mTiles.add(gbTile);

            updateTileVisibility();
        }
    };

    private static XC_MethodHook quickSettingsUpdateResourcesHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
            if (DEBUG) {
                log("updateResources - updating all tiles");
            }

            for (AQuickSettingsTile t : mTiles) {
                t.updateResources();
            }
        }
    };
}