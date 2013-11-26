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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ceco.gm2.gravitybox.Utils.MethodState;
import com.ceco.gm2.gravitybox.quicksettings.AQuickSettingsTile;
import com.ceco.gm2.gravitybox.quicksettings.ExpandedDesktopTile;
import com.ceco.gm2.gravitybox.quicksettings.GpsTile;
import com.ceco.gm2.gravitybox.quicksettings.NetworkModeTile;
import com.ceco.gm2.gravitybox.quicksettings.QuickAppTile;
import com.ceco.gm2.gravitybox.quicksettings.QuickRecordTile;
import com.ceco.gm2.gravitybox.quicksettings.RingerModeTile;
import com.ceco.gm2.gravitybox.quicksettings.ScreenshotTile;
import com.ceco.gm2.gravitybox.quicksettings.SleepTile;
import com.ceco.gm2.gravitybox.quicksettings.StayAwakeTile;
import com.ceco.gm2.gravitybox.quicksettings.TileOrderActivity;
import com.ceco.gm2.gravitybox.quicksettings.TorchTile;
import com.ceco.gm2.gravitybox.quicksettings.GravityBoxTile;
import com.ceco.gm2.gravitybox.quicksettings.SyncTile;
import com.ceco.gm2.gravitybox.quicksettings.VolumeTile;
import com.ceco.gm2.gravitybox.quicksettings.WifiApTile;
import com.ceco.gm2.gravitybox.quicksettings.WifiTile;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

public class ModQuickSettings {
    private static final String TAG = "GB:ModQuickSettings";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_QUICK_SETTINGS = "com.android.systemui.statusbar.phone.QuickSettings";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_PANEL_BAR = "com.android.systemui.statusbar.phone.PanelBar";
    private static final String CLASS_QS_TILEVIEW = "com.android.systemui.statusbar.phone.QuickSettingsTileView";
    private static final String CLASS_NOTIF_PANELVIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String CLASS_QS_CONTAINER_VIEW = "com.android.systemui.statusbar.phone.QuickSettingsContainerView";
    private static final String CLASS_QS_MODEL = "com.android.systemui.statusbar.phone.QuickSettingsModel";
    private static final String CLASS_QS_MODEL_RCB = "com.android.systemui.statusbar.phone.QuickSettingsModel$RefreshCallback";
    private static final String CLASS_QS_MODEL_STATE = "com.android.systemui.statusbar.phone.QuickSettingsModel.State";
    private static final String CLASS_ROTATION_LOCK_CTRL = "com.android.systemui.statusbar.policy.RotationLockController";
    private static final String CLASS_ROTATION_POLICY = "com.android.internal.view.RotationPolicy";
    private static final boolean DEBUG = false;

    private static final float STATUS_BAR_SETTINGS_FLIP_PERCENTAGE_RIGHT = 0.15f;
    private static final float STATUS_BAR_SETTINGS_FLIP_PERCENTAGE_LEFT = 0.85f;

    private static Context mContext;
    private static Context mGbContext;
    private static ViewGroup mContainerView;
    private static Object mPanelBar;
    private static Object mStatusBar;
    private static List<String> mActiveTileKeys;
    private static Class<?> mQuickSettingsTileViewClass;
    private static Object mSimSwitchPanelView;
    private static int mNumColumns = 3;
    private static int mLpOriginalHeight = -1;
    private static boolean mAutoSwitch = false;
    private static int mQuickPulldown = GravityBoxSettings.QUICK_PULLDOWN_OFF;
    private static Method methodGetColumnSpan;
    private static List<String> mCustomSystemTileKeys;
    private static List<Integer> mCustomGbTileKeys;
    private static Map<String, Integer> mAospTileTags;
    private static Object mQuickSettings;
    private static WifiManagerWrapper mWifiManager;
    private static Set<String> mOverrideTileKeys;
    private static XSharedPreferences mPrefs;

    private static ArrayList<AQuickSettingsTile> mTiles;
    private static Map<String, View> mAllTileViews;

    private static List<BroadcastSubReceiver> mBroadcastSubReceivers;

    static {
        mCustomSystemTileKeys = new ArrayList<String>(Arrays.asList(
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
            "auto_rotate_textview",
            "settings"
        ));

        mCustomGbTileKeys = new ArrayList<Integer>(Arrays.asList(
            R.id.sync_tileview,
            R.id.wifi_ap_tileview,
            R.id.gravitybox_tileview,
            R.id.torch_tileview,
            R.id.network_mode_tileview,
            R.id.sleep_tileview,
            R.id.quickapp_tileview,
            R.id.quickrecord_tileview,
            R.id.volume_tileview,
            R.id.expanded_tileview,
            R.id.stay_awake_tileview,
            R.id.screenshot_tileview,
            R.id.gps_tileview,
            R.id.ringer_mode_tileview
        ));
        if (Utils.isMtkDevice()) {
            mCustomGbTileKeys.add(R.id.wifi_tileview);
        }

        Map<String, Integer> tmpMap = new HashMap<String, Integer>();
        tmpMap.put("user_textview", 1);
        tmpMap.put("brightness_textview", 2);
        tmpMap.put("settings", 3);
        tmpMap.put("wifi_textview", 4);
        tmpMap.put("rssi_textview", 5);
        tmpMap.put("auto_rotate_textview", 6);
        tmpMap.put("battery_textview", 7);
        tmpMap.put("airplane_mode_textview", 8);
        tmpMap.put("bluetooth_textview", 9);
        tmpMap.put("gps_textview", 10);
        mAospTileTags = Collections.unmodifiableMap(tmpMap);

        mAllTileViews = new HashMap<String, View>();
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("received broadcast: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_QUICKSETTINGS_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_QS_PREFS)) {
                    mActiveTileKeys = new ArrayList<String>(Arrays.asList(
                            intent.getStringExtra(GravityBoxSettings.EXTRA_QS_PREFS).split(",")));
                    updateTileOrderAndVisibility();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_QS_COLS)) {
                    mNumColumns = intent.getIntExtra(GravityBoxSettings.EXTRA_QS_COLS, 3);
                    if (mContainerView != null) {
                        XposedHelpers.callMethod(mContainerView, "updateResources");
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_QS_AUTOSWITCH)) {
                    mAutoSwitch = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_QS_AUTOSWITCH, false);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_QUICK_PULLDOWN)) {
                    mQuickPulldown = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_QUICK_PULLDOWN, 
                            GravityBoxSettings.QUICK_PULLDOWN_OFF);
                }
            }

            for (BroadcastSubReceiver bsr : mBroadcastSubReceivers) {
                bsr.onBroadcastReceived(context, intent);
            }
        }
    };

    private static String getTileKey(View view) {
        if (view == null) return null;

        Resources res = mContext.getResources();
        for (String key : mCustomSystemTileKeys) {
            int resId = res.getIdentifier(key, "id", PACKAGE_NAME);
            if (view.findViewById(resId) != null || 
                    view.findViewWithTag(mAospTileTags.get(key)) != null) {
                return key;
            }
        }

        res = mGbContext.getResources();
        for (Integer key : mCustomGbTileKeys) {
            if (view.findViewById(key) != null) {
                return res.getResourceEntryName(key);
            }
        }

        return null;
    }

    private static void updateTileOrderAndVisibility() {
        if (mActiveTileKeys == null) {
            if (DEBUG) log("updateTileOrderAndVisibility: mActiveTileKeys is null - skipping");
            return;
        }

        try {
            final List<View> dynamicTiles = new ArrayList<View>();
    
            final int tileCount = mContainerView.getChildCount();
            for(int i = tileCount - 1; i >= 0; i--) {
                View view = mContainerView.getChildAt(i);
                final String key = getTileKey(view);
                if (key != null) {
                    if (!mAllTileViews.containsKey(key)) {
                        mAllTileViews.put(key, view);
                    }
                    mContainerView.removeView(view);
                } else if (view != null) {
                    // found tile that's not in our custom list
                    // might be dynamic tile (e.g. alarm) or some ROM specific tile?
                    // remove it and store it so it could be added in the end
                    dynamicTiles.add(view);
                    mContainerView.removeView(view);
                }
            }
    
            for (String key : mActiveTileKeys) {
                if (mAllTileViews.containsKey(key)) {
                    mContainerView.addView(mAllTileViews.get(key));
                }
            }
    
            // add tiles from dynamic list as last (e.g. alarm tile we previously removed)
            for (View v : dynamicTiles) {
                mContainerView.addView(v);
            }
    
            // trigger layout refresh
            XposedHelpers.callMethod(mContainerView, "updateResources");
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static TextView findTileTextView(ViewGroup viewGroup) {
        if (viewGroup == null) return null;

        TextView textView = null;
        final int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = viewGroup.getChildAt(i);
            if (childView instanceof ViewGroup) {
                textView = findTileTextView((ViewGroup) childView);
            } else if (childView instanceof TextView) {
                textView = (TextView) childView;
            }
            if (textView != null) {
                break;
            }
        }

        return textView;
    }

    private static void updateTileLayout(FrameLayout container, int orientation) {
        if (container == null) return;

        try {
            int textSize = 12;
            final Resources res = container.getResources();

            int imgMarginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    27, res.getDisplayMetrics());
            int imgMarginBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    17, res.getDisplayMetrics());
            try {
                imgMarginTop = res.getDimensionPixelSize(
                        res.getIdentifier("qs_tile_margin_above_icon", "dimen", PACKAGE_NAME));
                imgMarginBottom = res.getDimensionPixelSize(
                        res.getIdentifier("qs_tile_margin_below_icon", "dimen", PACKAGE_NAME));
            } catch (Resources.NotFoundException rnfe) {
                //
            }

            final int imgResId = res.getIdentifier("image", "id", PACKAGE_NAME);
            final int rssiImgResId = res.getIdentifier("rssi_image", "id", PACKAGE_NAME);

            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                switch (mNumColumns) {
                    case 4: 
                        textSize = 10;
                        imgMarginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                17, res.getDisplayMetrics());
                        imgMarginBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                11, res.getDisplayMetrics());
                        break;
                    case 5:
                        imgMarginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                10, res.getDisplayMetrics());
                        imgMarginBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                5, res.getDisplayMetrics());
                        textSize = 8; 
                        break;
                    case 3:
                    default:
                        textSize = 12;
                        break;
                }
            }

            final int tileCount = container.getChildCount();
            for(int i = 0; i < tileCount; i++) {
                ViewGroup viewGroup = (ViewGroup) mContainerView.getChildAt(i);
                if (viewGroup != null) {
                    TextView textView = findTileTextView(viewGroup);
                    if (textView != null) {
                        textView.setTextSize(1, textSize);
                        textView.setSingleLine(false);
                        textView.setAllCaps(true);
                    }
    
                    // adjust layout in case it's AOSP 4.3+ tile
                    if (Build.VERSION.SDK_INT > 17 && imgResId != 0 && rssiImgResId != 0) {
                        View img = viewGroup.findViewById(imgResId);
                        if (img != null) {
                            // basic tile
                            if (img.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) img.getLayoutParams();
                                lp.topMargin = imgMarginTop;
                                lp.bottomMargin = imgMarginBottom;
                                img.setLayoutParams(lp);
                                img.requestLayout();
                            }
                        } else {
                            // RSSI special tile
                            img = viewGroup.findViewById(rssiImgResId);
                            if (img != null && img.getParent() instanceof FrameLayout) {
                                FrameLayout fl = (FrameLayout) img.getParent();
                                if (fl.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fl.getLayoutParams();
                                    lp.topMargin = imgMarginTop;
                                    lp.bottomMargin = imgMarginBottom;
                                    fl.setLayoutParams(lp);
                                    fl.requestLayout();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void initResources(XSharedPreferences prefs, InitPackageResourcesParam resparam) {
        try {
            // Enable rotation lock tile for non-MTK devices
            if (!Utils.isMtkDevice()) {
                resparam.res.setReplacement(PACKAGE_NAME, "bool", "quick_settings_show_rotation_lock", true);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        if (DEBUG) log("init");

        try {
            final ThreadLocal<MethodState> removeNotificationState = 
                    new ThreadLocal<MethodState>();
            removeNotificationState.set(MethodState.UNKNOWN);

            mPrefs = prefs;
            mPrefs.reload();
            String tileKeys = mPrefs.getString(TileOrderActivity.PREF_KEY_TILE_ORDER, null);
            if (tileKeys != null) {
                mActiveTileKeys = new ArrayList<String>(Arrays.asList(mPrefs.getString(
                        TileOrderActivity.PREF_KEY_TILE_ORDER, "").split(",")));
            }
            if (DEBUG) log("got tile prefs: mActiveTileKeys = " + 
                    (mActiveTileKeys == null ? "null" : mActiveTileKeys.toString()));
            mOverrideTileKeys = mPrefs.getStringSet(
                    GravityBoxSettings.PREF_KEY_QS_TILE_BEHAVIOUR_OVERRIDE, new HashSet<String>());
            if (DEBUG) log("got tile override prefs: mOverrideTileKeys = " +
                    (mOverrideTileKeys == null ? "null" : mOverrideTileKeys.toString()));

            try {
                mNumColumns = Integer.valueOf(mPrefs.getString(
                        GravityBoxSettings.PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW, "3"));
            } catch (NumberFormatException e) {
                log("Invalid preference for tiles per row: " + e.getMessage());
            }

            mAutoSwitch = mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_QUICK_SETTINGS_AUTOSWITCH, false);

            try {
                mQuickPulldown = Integer.valueOf(mPrefs.getString(
                        GravityBoxSettings.PREF_KEY_QUICK_PULLDOWN, "0"));
            } catch (NumberFormatException e) {
                log("Invalid preference for quick pulldown: " + e.getMessage());
            }

            final Class<?> quickSettingsClass = XposedHelpers.findClass(CLASS_QUICK_SETTINGS, classLoader);
            final Class<?> phoneStatusBarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> panelBarClass = XposedHelpers.findClass(CLASS_PANEL_BAR, classLoader);
            mQuickSettingsTileViewClass = XposedHelpers.findClass(CLASS_QS_TILEVIEW, classLoader);
            methodGetColumnSpan = mQuickSettingsTileViewClass.getDeclaredMethod("getColumnSpan");
            final Class<?> notifPanelViewClass = XposedHelpers.findClass(CLASS_NOTIF_PANELVIEW, classLoader);
            final Class<?> quickSettingsContainerViewClass = XposedHelpers.findClass(CLASS_QS_CONTAINER_VIEW, classLoader);

            XposedBridge.hookAllConstructors(quickSettingsClass, quickSettingsConstructHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "setBar", 
                    panelBarClass, quickSettingsSetBarHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "setService", 
                    phoneStatusBarClass, quickSettingsSetServiceHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "addSystemTiles", 
                    ViewGroup.class, LayoutInflater.class, quickSettingsAddSystemTilesHook);
            XposedHelpers.findAndHookMethod(quickSettingsClass, "updateResources", quickSettingsUpdateResourcesHook);
            XposedHelpers.findAndHookMethod(notifPanelViewClass, "onTouchEvent", 
                    MotionEvent.class, notificationPanelViewOnTouchEvent);
            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "makeStatusBarView", 
                    makeStatusBarViewHook);
            XposedHelpers.findAndHookMethod(quickSettingsContainerViewClass, "updateResources", 
                    qsContainerViewUpdateResources);
            XposedHelpers.findAndHookMethod(quickSettingsContainerViewClass, "onMeasure",
                    int.class, int.class, qsContainerViewOnMeasure);

            // tag AOSP QS views for future identification
            if (!Utils.isMtkDevice()) {
                tagAospTileViews(classLoader);
            }

            if (Build.VERSION.SDK_INT > 18) {
                final Class<?> rlControllerClass = XposedHelpers.findClass(CLASS_ROTATION_LOCK_CTRL, classLoader);
                XposedHelpers.findAndHookMethod(rlControllerClass, "isRotationLockAffordanceVisible", 
                        new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            return mActiveTileKeys != null ? 
                                    mActiveTileKeys.contains("auto_rotate_textview") :
                                    XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        }
                });
                final Class<?> rlPolicyClass = XposedHelpers.findClass(CLASS_ROTATION_POLICY, null);
                XposedHelpers.findAndHookMethod(rlPolicyClass, "isRotationLockToggleSupported",
                        Context.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                return XposedHelpers.callStaticMethod(rlPolicyClass, "isRotationSupported", param.args[0]);
                            } catch (Throwable t) {
                                XposedBridge.log(t);
                                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                            }
                        }
                });
            }

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "removeNotification", IBinder.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) log("removeNotification method ENTER");
                    removeNotificationState.set(MethodState.METHOD_ENTERED);
                }

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) log("removeNotification method EXIT");
                    removeNotificationState.set(MethodState.METHOD_EXITED);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "animateCollapsePanels", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (removeNotificationState.get().equals(MethodState.METHOD_ENTERED)) {
                        if (DEBUG) log("animateCollapsePanels called from removeNotification method");

                        boolean hasFlipSettings = XposedHelpers.getBooleanField(param.thisObject, "mHasFlipSettings");
                        boolean animating = Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 ? false : 
                                XposedHelpers.getBooleanField(param.thisObject, "mAnimating");
                        View flipSettingsView = (View) XposedHelpers.getObjectField(param.thisObject, "mFlipSettingsView");
                        Object notificationData = XposedHelpers.getObjectField(mStatusBar, "mNotificationData");
                        int ndSize = (Integer) XposedHelpers.callMethod(notificationData, "size");
                        boolean isShowingSettings = hasFlipSettings && flipSettingsView.getVisibility() == View.VISIBLE;

                        if (ndSize == 0 && !animating && !isShowingSettings) {
                            // let the original method finish its work
                        } else {
                            if (DEBUG) log("animateCollapsePanels: all notifications removed " +
                                    "but showing QuickSettings - do nothing");
                            param.setResult(null);
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook quickSettingsConstructHook = new XC_MethodHook() {

        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (DEBUG) log("QuickSettings constructed - initializing local members");

            mQuickSettings = param.thisObject;
            mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            mGbContext = mContext.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            mContainerView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mContainerView");
            mWifiManager = new WifiManagerWrapper(mContext);

            IntentFilter intentFilter = new IntentFilter(GravityBoxSettings.ACTION_PREF_QUICKSETTINGS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_QUICKAPP_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    };

    private static XC_MethodHook quickSettingsSetBarHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            mPanelBar = param.args[0];
            if (DEBUG) log("mPanelBar set");
        }
    };

    private static XC_MethodHook quickSettingsSetServiceHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            mStatusBar = param.args[0];
            if (DEBUG) log("mStatusBar set");
        }
    };

    private static XC_MethodHook quickSettingsAddSystemTilesHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (DEBUG) log("about to add tiles");

            try {
                LayoutInflater inflater = (LayoutInflater) param.args[1];

                mTiles = new ArrayList<AQuickSettingsTile>();

                if (Utils.isMtkDevice()) {
                    WifiTile wt = new WifiTile(mContext, mGbContext, mStatusBar, mPanelBar, mWifiManager);
                    wt.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                    mTiles.add(wt);
                }

                if (Utils.hasGPS(mContext)) {
                    GpsTile gpsTile = new GpsTile(mContext, mGbContext, mStatusBar, mPanelBar);
                    gpsTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                    mTiles.add(gpsTile);
                }

                RingerModeTile rmTile = new RingerModeTile(mContext, mGbContext, mStatusBar, mPanelBar);
                rmTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(rmTile);

                VolumeTile volTile = new VolumeTile(mContext, mGbContext, mStatusBar, mPanelBar);
                volTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(volTile);

                if (!Utils.isWifiOnly(mContext)) {
                    NetworkModeTile nmTile = new NetworkModeTile(mContext, mGbContext, mStatusBar, mPanelBar);
                    nmTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                    mTiles.add(nmTile);
                }

                SyncTile syncTile = new SyncTile(mContext, mGbContext, mStatusBar, mPanelBar);
                syncTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(syncTile);

                WifiApTile wifiApTile = new WifiApTile(mContext, mGbContext, mStatusBar, mPanelBar, mWifiManager);
                wifiApTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(wifiApTile);

                if (Utils.hasFlash(mContext)) {
                    TorchTile torchTile = new TorchTile(mContext, mGbContext, mStatusBar, mPanelBar);
                    torchTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                    mTiles.add(torchTile);
                }

                SleepTile sleepTile = new SleepTile(mContext, mGbContext, mStatusBar, mPanelBar);
                sleepTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(sleepTile);

                StayAwakeTile swTile = new StayAwakeTile(mContext, mGbContext, mStatusBar, mPanelBar);
                swTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(swTile);

                QuickRecordTile qrTile = new QuickRecordTile(mContext, mGbContext, mStatusBar, mPanelBar);
                qrTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(qrTile);

                QuickAppTile qAppTile = new QuickAppTile(mContext, mGbContext, mStatusBar, mPanelBar);
                qAppTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(qAppTile);

                ExpandedDesktopTile edTile = new ExpandedDesktopTile(mContext, mGbContext, mStatusBar, mPanelBar);
                edTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(edTile);

                ScreenshotTile ssTile = new ScreenshotTile(mContext, mGbContext, mStatusBar, mPanelBar);
                ssTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(ssTile);

                GravityBoxTile gbTile = new GravityBoxTile(mContext, mGbContext, mStatusBar, mPanelBar);
                gbTile.setupQuickSettingsTile(mContainerView, inflater, mPrefs);
                mTiles.add(gbTile);

                mBroadcastSubReceivers = new ArrayList<BroadcastSubReceiver>();
                for (AQuickSettingsTile t : mTiles) {
                    mBroadcastSubReceivers.add(t);
                }

                updateTileOrderAndVisibility();
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static XC_MethodHook quickSettingsUpdateResourcesHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
            if (DEBUG) log("updateResources - updating all tiles");

            for (AQuickSettingsTile t : mTiles) {
                t.updateResources();
            }
        }
    };

    private static XC_MethodReplacement notificationPanelViewOnTouchEvent = new XC_MethodReplacement() {

        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            try {
                MotionEvent event = (MotionEvent) param.args[0];

                if (mStatusBar != null && XposedHelpers.getBooleanField(mStatusBar, "mHasFlipSettings")) {
                    boolean shouldFlip = false;
                    boolean okToFlip = XposedHelpers.getBooleanField(param.thisObject, "mOkToFlip");
                    Object notificationData = XposedHelpers.getObjectField(mStatusBar, "mNotificationData");
                    float handleBarHeight = XposedHelpers.getFloatField(param.thisObject, "mHandleBarHeight");
                    Method getExpandedHeight = param.thisObject.getClass().getSuperclass().getMethod("getExpandedHeight");
                    float expandedHeight = (Float) getExpandedHeight.invoke(param.thisObject);
                    final int width = ((View) param.thisObject).getWidth();
                    
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            okToFlip = (expandedHeight == 0);
                            XposedHelpers.setBooleanField(param.thisObject, "mOkToFlip", okToFlip);
                            if (mAutoSwitch && 
                                    (Integer)XposedHelpers.callMethod(notificationData, "size") == 0 &&
                                    !isSimSwitchPanelShowing()) {
                                shouldFlip = true;
                            } else if (mQuickPulldown == GravityBoxSettings.QUICK_PULLDOWN_RIGHT
                                        && (event.getX(0) > (width * 
                                        (1.0f - STATUS_BAR_SETTINGS_FLIP_PERCENTAGE_RIGHT)))) {
                                shouldFlip = true;
                            } else if (mQuickPulldown == GravityBoxSettings.QUICK_PULLDOWN_LEFT
                                        && (event.getX(0) < (width *
                                        (1.0f - STATUS_BAR_SETTINGS_FLIP_PERCENTAGE_LEFT)))) {
                                shouldFlip = true;
                            }
                            break;
                        case MotionEvent.ACTION_POINTER_DOWN:
                            if (okToFlip) {
                                float miny = event.getY(0);
                                float maxy = miny;
                                for (int i = 1; i < event.getPointerCount(); i++) {
                                    final float y = event.getY(i);
                                    if (y < miny) miny = y;
                                    if (y > maxy) maxy = y;
                                }
                                if (maxy - miny < handleBarHeight) {
                                    shouldFlip = true;
                                }
                            }
                            break;
                    }
                    if (okToFlip && shouldFlip) {
                        if (expandedHeight < handleBarHeight) {
                            XposedHelpers.callMethod(mStatusBar, "switchToSettings");
                        } else {
                            XposedHelpers.callMethod(mStatusBar, "flipToSettings");
                        }
                        okToFlip = false;
                    }
                }

                View handleView = (View) XposedHelpers.getObjectField(param.thisObject, "mHandleView"); 
                return handleView.dispatchTouchEvent(event);
            } catch (Throwable t) {
                XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                XposedBridge.log(t);
                return null;
            }
        }
    };

    private static XC_MethodHook makeStatusBarViewHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (Utils.isMtkDevice()) {
                try {
                    Object toolbarView = XposedHelpers.getObjectField(param.thisObject, "mToolBarView");
                    if (toolbarView != null) {
                        mSimSwitchPanelView = XposedHelpers.getObjectField(toolbarView, "mSimSwitchPanelView");
                        if (DEBUG) log("makeStatusBarView: SimSwitchPanelView found");
                    }
                } catch (NoSuchFieldError e) {
                    //
                }
            }
        }
    };

    private static XC_MethodHook qsContainerViewUpdateResources = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            if (DEBUG) log("qsContainerView updateResources called");
            // do this only for portrait mode
            FrameLayout fl = (FrameLayout) param.thisObject;
            final int orientation = fl.getContext().getResources().getConfiguration().orientation;
            updateTileLayout(fl, orientation);
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                XposedHelpers.setIntField(param.thisObject, "mNumColumns", mNumColumns);
                fl.requestLayout();
            }
        }
    };

    private static XC_MethodReplacement qsContainerViewOnMeasure = new XC_MethodReplacement() {

        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            try {
                ViewGroup thisView = (ViewGroup) param.thisObject;
                int widthMeasureSpec = (Integer) param.args[0];
                int heightMeasureSpec = (Integer) param.args[1];
                float mCellGap = XposedHelpers.getFloatField(thisView, "mCellGap");
                int numColumns = XposedHelpers.getIntField(thisView, "mNumColumns");
                int orientation = mContext.getResources().getConfiguration().orientation;
                
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int height = MeasureSpec.getSize(heightMeasureSpec);
                int availableWidth = (int) (width - thisView.getPaddingLeft() - thisView.getPaddingRight() -
                        (numColumns - 1) * mCellGap);
                float cellWidth = (float) Math.ceil(((float) availableWidth) / numColumns);
    
                // Update each of the children's widths accordingly to the cell width
                int N = thisView.getChildCount();
                int cellHeight = 0;
                int cursor = 0;
                for (int i = 0; i < N; ++i) {
                    // Update the child's width
                    View v = (View) thisView.getChildAt(i);
                    if (v.getVisibility() != View.GONE) {
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                        int colSpan = (Integer) methodGetColumnSpan.invoke(v);
                        lp.width = (int) ((colSpan * cellWidth) + (colSpan - 1) * mCellGap);
    
                        if (mLpOriginalHeight == -1) mLpOriginalHeight = lp.height;
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                            if (numColumns > 3) {
                                lp.height = (lp.width * numColumns-1) / numColumns;
                            } else {
                                lp.height = mLpOriginalHeight;
                            }
                        } else {
                            lp.height = mLpOriginalHeight;
                        }
    
                        // Measure the child
                        int newWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
                        int newHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                        v.measure(newWidthSpec, newHeightSpec);
    
                        // Save the cell height
                        if (cellHeight <= 0) {
                            cellHeight = v.getMeasuredHeight();
                        }
                        cursor += colSpan;
                    }
                }
    
                // Set the measured dimensions.  We always fill the tray width, but wrap to the height of
                // all the tiles.
                // Calling to setMeasuredDimension is protected final and not accessible directly from here
                // so we emulate it
                int numRows = (int) Math.ceil((float) cursor / numColumns);
                int newHeight = (int) ((numRows * cellHeight) + ((numRows - 1) * mCellGap)) +
                        thisView.getPaddingTop() + thisView.getPaddingBottom();
    
                Field fMeasuredWidth = View.class.getDeclaredField("mMeasuredWidth");
                fMeasuredWidth.setAccessible(true);
                Field fMeasuredHeight = View.class.getDeclaredField("mMeasuredHeight");
                fMeasuredHeight.setAccessible(true);
                Field fPrivateFlags = View.class.getDeclaredField("mPrivateFlags");
                fPrivateFlags.setAccessible(true); 
                fMeasuredWidth.setInt(thisView, width);
                fMeasuredHeight.setInt(thisView, newHeight);
                int privateFlags = fPrivateFlags.getInt(thisView);
                privateFlags |= 0x00000800;
                fPrivateFlags.setInt(thisView, privateFlags);
    
                return null;
            } catch (Throwable t) {
                // fallback to original method in case of problems
                XposedBridge.log(t);
                XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                return null;
            }
        }
    };

    private static boolean isSimSwitchPanelShowing() {
        if (mSimSwitchPanelView == null) return false;

        return (Boolean) XposedHelpers.callMethod(mSimSwitchPanelView, "isPanelShowing");
    }

    private static void tagAospTileViews(ClassLoader classLoader) {
        final Class<?> classQsModel = XposedHelpers.findClass(CLASS_QS_MODEL, classLoader);

        try {
            XposedHelpers.findAndHookMethod(classQsModel, "addUserTile",
                    CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    ((View)param.args[0]).setTag(mAospTileTags.get("user_textview"));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(classQsModel, "addBrightnessTile",
                    CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    ((View)param.args[0]).setTag(mAospTileTags.get("brightness_textview"));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(classQsModel, "addSettingsTile",
                    CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    ((View)param.args[0]).setTag(mAospTileTags.get("settings"));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(classQsModel, "addWifiTile",
                    CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final View tile = (View) param.args[0];
                    tile.setTag(mAospTileTags.get("wifi_textview"));
                    if (mOverrideTileKeys.contains("wifi_textview")) {
                        tile.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mWifiManager.toggleWifiEnabled();
                            }
                        });
                        tile.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                XposedHelpers.callMethod(mQuickSettings, "startSettingsActivity", 
                                        android.provider.Settings.ACTION_WIFI_SETTINGS);
                                tile.setPressed(false);
                                return true;
                            }
                        });
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(classQsModel, "addRSSITile",
                    CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final View tile = (View) param.args[0];
                    tile.setTag(mAospTileTags.get("rssi_textview"));
                    if (mOverrideTileKeys.contains("rssi_textview")) {
                        tile.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final ConnectivityManager cm = 
                                       (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                                final boolean mobileDataEnabled = 
                                        (Boolean) XposedHelpers.callMethod(cm, "getMobileDataEnabled");

                                if (Utils.isXperiaDevice()) {
                                    if (!mobileDataEnabled && mStatusBar != null) {
                                        XposedHelpers.callMethod(mStatusBar, "animateCollapsePanels");
                                    }

                                    Intent i = new Intent(ConnectivityServiceWrapper.ACTION_XPERIA_MOBILE_DATA_TOGGLE);
                                    mContext.sendBroadcast(i);
                                    return;
                                }

                                Intent intent = new Intent(ConnectivityServiceWrapper.ACTION_SET_MOBILE_DATA_ENABLED);
                                intent.putExtra(ConnectivityServiceWrapper.EXTRA_ENABLED, !mobileDataEnabled);
                                mContext.sendBroadcast(intent);
                            }
                        });
                        tile.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName("com.android.settings", 
                                        "com.android.settings.Settings$DataUsageSummaryActivity"));
                                XposedHelpers.callMethod(mQuickSettings, "startSettingsActivity", intent);
                                tile.setPressed(false);
                                return true;
                            }
                        });
                        XposedHelpers.findAndHookMethod(param.args[1].getClass(), "refreshView",
                                CLASS_QS_TILEVIEW, CLASS_QS_MODEL_STATE, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(final MethodHookParam param2) throws Throwable {
                                if (param2.args[0] != tile) return;
                                final ConnectivityManager cm = 
                                        (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                                if (!(Boolean) XposedHelpers.callMethod(cm, "getMobileDataEnabled")) {
                                    ImageView iov = (ImageView) tile.findViewById(
                                            mContext.getResources().getIdentifier(
                                                    "rssi_overlay_image", "id", PACKAGE_NAME));
                                    if (iov != null) {
                                        iov.setImageDrawable(mGbContext.getResources().getDrawable(
                                                R.drawable.ic_qs_signal_data_off));
                                    }
                                }
                            }
                        });
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            final XC_MethodHook addRotationLockTileHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    ((View)param.args[0]).setTag(mAospTileTags.get("auto_rotate_textview"));
                }
            };
            if (Build.VERSION.SDK_INT > 18) {
                XposedHelpers.findAndHookMethod(classQsModel, "addRotationLockTile",
                        CLASS_QS_TILEVIEW, CLASS_ROTATION_LOCK_CTRL, CLASS_QS_MODEL_RCB, addRotationLockTileHook);
            } else {
                XposedHelpers.findAndHookMethod(classQsModel, "addRotationLockTile",
                        CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, addRotationLockTileHook);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(classQsModel, "addBatteryTile",
                    CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    ((View)param.args[0]).setTag(mAospTileTags.get("battery_textview"));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(classQsModel, "addAirplaneModeTile",
                    CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final View tile = (View) param.args[0];
                    tile.setTag(mAospTileTags.get("airplane_mode_textview"));
                    if (mOverrideTileKeys.contains("airplane_mode_textview")) {
                        tile.setOnClickListener(new View.OnClickListener() {
                            @SuppressLint("NewApi")
                            @Override
                            public void onClick(View v) {
                                final ContentResolver cr = mContext.getContentResolver();
                                final boolean amOn = Settings.Global.getInt(cr,
                                        Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
                                Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON,
                                        amOn ? 0 : 1);
                                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                                intent.putExtra("state", !amOn);
                                mContext.sendBroadcast(intent);
                            }
                        });
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(classQsModel, "addBluetoothTile",
                    CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final View tile = (View) param.args[0];
                    tile.setTag(mAospTileTags.get("bluetooth_textview"));
                    if (mOverrideTileKeys.contains("bluetooth_textview")) {
                        tile.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                                if (btAdapter.isEnabled()) {
                                    btAdapter.disable();
                                } else {
                                    btAdapter.enable();
                                }
                            }
                        });
                        tile.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                XposedHelpers.callMethod(mQuickSettings, "startSettingsActivity", 
                                        android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                                tile.setPressed(false);
                                return true;
                            }
                        });
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (Build.VERSION.SDK_INT > 18) {
            try {
                XposedHelpers.findAndHookMethod(classQsModel, "addLocationTile",
                        CLASS_QS_TILEVIEW, CLASS_QS_MODEL_RCB, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        ((View)param.args[0]).setTag(mAospTileTags.get("gps_textview"));
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }
}