package com.ceco.gm2.gravitybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class ModStatusbarColor {
    private static final String TAG = "ModStatusbarColor";
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_PANEL_BAR = "com.android.systemui.statusbar.phone.PanelBar";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_SIGNAL_CLUSTER_VIEW = Utils.isMtkDevice() ? 
            "com.android.systemui.statusbar.SignalClusterViewGemini" :
            "com.android.systemui.statusbar.SignalClusterView";
    private static final String CLASS_BATTERY_CONTROLLER = "com.android.systemui.statusbar.policy.BatteryController";
    private static final String CLASS_NOTIF_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";

    private static View mPanelBar;
    private static StatusBarIconManager mIconManager;
    private static Object mSignalClusterView;
    private static boolean mIconColorEnabled;
    private static TextView mClock;
    private static CmCircleBattery mCircleBattery;
    private static TextView mPercentage;
    private static ImageView mBattery;
    private static int mBatteryLevel;
    private static boolean mBatteryPlugged;
    private static Object mBatteryController;
    private static FrameLayout mNotificationPanelView;
    private static NotificationWallpaper mNotificationWallpaper;
    private static Integer mClockDefaultColor;
    private static Integer mPercentageDefaultColor;
    private static boolean mRoamingIndicatorsDisabled;
    private static TransparencyManager mTransparencyManager;

    static {
        mIconManager = new StatusBarIconManager(XModuleResources.createInstance(GravityBox.MODULE_PATH, null));
        mIconColorEnabled = false;
        mBatteryLevel = 0;
        mBatteryPlugged = false;
        mClockDefaultColor = null;
        mPercentageDefaultColor = null;
        mRoamingIndicatorsDisabled = false;
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void setClock(TextView clock) {
        mClock = clock;
    }

    public static void setCircleBattery(CmCircleBattery circleBattery) {
        mCircleBattery = circleBattery;
    }

    public static void setPercentage(TextView percentage) {
        mPercentage = percentage;
    }

    public static void setBattery(ImageView battery) {
        mBattery = battery;
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            log("received broadcast: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_BG_COLOR)) {
                    int bgColor = intent.getIntExtra(GravityBoxSettings.EXTRA_SB_BG_COLOR, Color.BLACK);
                    setStatusbarBgColor(bgColor);
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_ICON_COLOR)) {
                    int iconColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_SB_ICON_COLOR, StatusBarIconManager.DEFAULT_ICON_COLOR);
                    mIconManager.setIconColor(iconColor);
                    applyIconColors();
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR)) {
                    int daColor = intent.getIntExtra(
                            GravityBoxSettings.EXTRA_SB_DATA_ACTIVITY_COLOR, 
                            StatusBarIconManager.DEFAULT_DATA_ACTIVITY_COLOR);
                    mIconManager.setDataActivityColor(daColor);
                    applyIconColors();
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_SB_ICON_COLOR_ENABLE)) {
                    mIconColorEnabled = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_SB_ICON_COLOR_ENABLE, false);
                    log("Icon colors master switch set to: " + mIconColorEnabled);
                    if (!mIconColorEnabled) mIconManager.clearCache();
                    applyIconColors();
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_SB_LAUNCHER)) {
                    Settings.System.putInt(context.getContentResolver(),
                            TransparencyManager.SETTING_STATUS_BAR_ALPHA_CONFIG_LAUNCHER,
                            intent.getIntExtra(GravityBoxSettings.EXTRA_TM_SB_LAUNCHER, 0));
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_SB_LOCKSCREEN)) {
                    Settings.System.putInt(context.getContentResolver(),
                            TransparencyManager.SETTING_STATUS_BAR_ALPHA_CONFIG_LOCKSCREEN,
                            intent.getIntExtra(GravityBoxSettings.EXTRA_TM_SB_LOCKSCREEN, 0));
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_NB_LAUNCHER)) {
                    Settings.System.putInt(context.getContentResolver(),
                            TransparencyManager.SETTING_NAVIGATION_BAR_ALPHA_CONFIG_LAUNCHER,
                            intent.getIntExtra(GravityBoxSettings.EXTRA_TM_NB_LAUNCHER, 0));
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_TM_NB_LOCKSCREEN)) {
                    Settings.System.putInt(context.getContentResolver(),
                            TransparencyManager.SETTING_NAVIGATION_BAR_ALPHA_CONFIG_LOCKSCREEN,
                            intent.getIntExtra(GravityBoxSettings.EXTRA_TM_NB_LOCKSCREEN, 0));
                }
            }

            if (intent.getAction().equals(GravityBoxSettings.ACTION_NOTIF_BACKGROUND_CHANGED) &&
                    mNotificationWallpaper != null) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_TYPE)) {
                    mNotificationWallpaper.setType(
                            intent.getStringExtra(GravityBoxSettings.EXTRA_BG_TYPE));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_COLOR)) {
                    mNotificationWallpaper.setColor(
                            intent.getIntExtra(GravityBoxSettings.EXTRA_BG_COLOR, Color.BLACK));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_ALPHA)) {
                    mNotificationWallpaper.setAlpha(
                            intent.getIntExtra(GravityBoxSettings.EXTRA_BG_ALPHA, 60));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_BG_COLOR_MODE)) {
                    mNotificationWallpaper.setColorMode(
                            intent.getStringExtra(GravityBoxSettings.EXTRA_BG_COLOR_MODE));
                }
                updateNotificationPanelBackground();
            }

            if (intent.getAction().equals(GravityBoxSettings.ACTION_DISABLE_ROAMING_INDICATORS_CHANGED)) {
                mRoamingIndicatorsDisabled = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_INDICATORS_DISABLED, false);
            }
        }
    };

    public static void initZygote() {
        try {
            final Class<?> phoneWindowManagerClass = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);

            log("replacing getSystemDecorRectLw method");
            XposedHelpers.findAndHookMethod(phoneWindowManagerClass,
                    "getSystemDecorRectLw", Rect.class, new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Rect rect = (Rect) param.args[0];
                            rect.left = XposedHelpers.getIntField(param.thisObject, "mSystemLeft");
                            rect.top = XposedHelpers.getIntField(param.thisObject, "mSystemTop");
                            rect.right = XposedHelpers.getIntField(param.thisObject, "mSystemRight");
                            rect.bottom = XposedHelpers.getIntField(param.thisObject, "mSystemBottom");
                            return 0;
                        }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> panelBarClass = XposedHelpers.findClass(CLASS_PANEL_BAR, classLoader);
            final Class<?> phoneStatusbarClass = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> signalClusterViewClass = XposedHelpers.findClass(CLASS_SIGNAL_CLUSTER_VIEW, classLoader);
            final Class<?> batteryControllerClass = XposedHelpers.findClass(CLASS_BATTERY_CONTROLLER, classLoader);
            final Class<?> notifPanelViewClass = XposedHelpers.findClass(CLASS_NOTIF_PANEL_VIEW, classLoader);

            mIconColorEnabled = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE, false);
            mIconManager.setIconColor(
                    prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_ICON_COLOR,
                            StatusBarIconManager.DEFAULT_ICON_COLOR));
            mIconManager.setDataActivityColor(
                    prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR, 
                            StatusBarIconManager.DEFAULT_DATA_ACTIVITY_COLOR));
            mRoamingIndicatorsDisabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_DISABLE_ROAMING_INDICATORS, false);

            XposedBridge.hookAllConstructors(panelBarClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPanelBar = (View) param.thisObject;

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_NOTIF_BACKGROUND_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_DISABLE_ROAMING_INDICATORS_CHANGED);
                    mPanelBar.getContext().registerReceiver(mBroadcastReceiver, intentFilter);
                }
            });

            XposedBridge.hookAllConstructors(signalClusterViewClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mSignalClusterView = param.thisObject;
                    log("SignalClusterView constructed - mSignalClusterView set");
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, 
                    "makeStatusBarView", new XC_MethodHook(XCallback.PRIORITY_LOWEST) {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    mTransparencyManager = new TransparencyManager(context);
                    mTransparencyManager.setStatusbar(XposedHelpers.getObjectField(param.thisObject, "mStatusBarView"));
                    mTransparencyManager.setNavbar(XposedHelpers.getObjectField(
                            param.thisObject, "mNavigationBarView"));

                    mBatteryController = XposedHelpers.getObjectField(param.thisObject, "mBatteryController");
                    prefs.reload();
                    int bgColor = prefs.getInt(GravityBoxSettings.PREF_KEY_STATUSBAR_BGCOLOR, Color.BLACK);
                    setStatusbarBgColor(bgColor);
                    applyIconColors();
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "getNavigationBarLayoutParams", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.getResult();
                    if (lp != null) {
                        lp.format = PixelFormat.TRANSLUCENT;
                        param.setResult(lp);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "disable", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mTransparencyManager != null) {
                        mTransparencyManager.update();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusbarClass, "topAppWindowChanged",
                    boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mTransparencyManager != null) {
                        mTransparencyManager.update();
                    }
                }
            });

            XposedHelpers.findAndHookMethod(batteryControllerClass, "onReceive",
                    Context.class, Intent.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[1];
                    if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                        mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                        mBatteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
                        if (mIconColorEnabled && mBattery != null) {
                            Drawable d = mIconManager.getBatteryIcon(mBatteryLevel, mBatteryPlugged);
                            if (d != null) mBattery.setImageDrawable(d);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(signalClusterViewClass, "apply", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (XposedHelpers.getObjectField(param.thisObject, "mWifiGroup") == null) return;

                    Resources res = ((LinearLayout) param.thisObject).getContext().getResources();

                    if (mIconColorEnabled) {
                        Object[] mobileIconIds = null, mobileIconIdsGemini = null;
                        Object mobileActivityId = null, mobileActivityIdGemini = null;
                        Object mobileTypeId = null, mobileTypeIdGemini = null;
                        if (Utils.isMtkDevice()) {
                            mobileIconIds = (Object[]) XposedHelpers.getObjectField(param.thisObject, "mMobileStrengthId");
                            mobileIconIdsGemini = (Object[]) XposedHelpers.getObjectField(param.thisObject, "mMobileStrengthIdGemini");
                            mobileActivityId = XposedHelpers.getObjectField(param.thisObject, "mMobileActivityId");
                            mobileActivityIdGemini = XposedHelpers.getObjectField(param.thisObject, "mMobileActivityIdGemini");
                            mobileTypeId = XposedHelpers.getObjectField(param.thisObject, "mMobileTypeId");
                            mobileTypeIdGemini = XposedHelpers.getObjectField(param.thisObject, "mMobileTypeIdGemini");
                        }
    
                        if (XposedHelpers.getBooleanField(param.thisObject, "mWifiVisible")) {
                            ImageView wifiIcon = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mWifi");
                            if (wifiIcon != null) {
                                try {
                                    int resId = XposedHelpers.getIntField(param.thisObject, "mWifiStrengthId");
                                    String resName = res.getResourceEntryName(resId);
                                    Drawable d = mIconManager.getWifiIcon(resName);
                                    if (d != null) wifiIcon.setImageDrawable(d);
                                } catch (Resources.NotFoundException e) { }
                            }
                            ImageView wifiActivity = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mWifiActivity");
                            if (wifiActivity != null) {
                                try {
                                    int resId = XposedHelpers.getIntField(param.thisObject, "mWifiActivityId");
                                    Drawable d = res.getDrawable(resId).mutate();
                                    d = mIconManager.applyDataActivityColorFilter(d);
                                    wifiActivity.setImageDrawable(d);
                                } catch (Resources.NotFoundException e) {
                                    wifiActivity.setImageDrawable(null);
                                }
                            }
                        }
    
                        if (!XposedHelpers.getBooleanField(param.thisObject, "mIsAirplaneMode")) {
                            // for SIM Slot 1
                            if (XposedHelpers.getBooleanField(param.thisObject, "mMobileVisible")) {
                                boolean allowChange = false;
                                ImageView mobile = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobile");
                                if (mobile != null) {
                                    try {
                                        int resId = Utils.isMtkDevice() ? 
                                                (Integer) XposedHelpers.callMethod(mobileIconIds[0], "getIconId") :
                                                XposedHelpers.getIntField(param.thisObject, "mMobileStrengthId");
                                        String resName = res.getResourceEntryName(resId);
                                        allowChange = resName.contains("blue") | !Utils.isMtkDevice();
                                        Drawable d = mIconManager.getMobileIcon(resName);
                                        if (d != null) mobile.setImageDrawable(d);
                                    } catch (Resources.NotFoundException e) { }
                                }
                                if (allowChange) {
                                    ImageView mobileActivity = 
                                            (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileActivity");
                                    if (mobileActivity != null) {
                                        try {
                                            int resId = Utils.isMtkDevice() ? 
                                                    (Integer) XposedHelpers.callMethod(mobileActivityId, "getIconId") :
                                                    XposedHelpers.getIntField(param.thisObject, "mMobileActivityId");
                                            Drawable d = res.getDrawable(resId).mutate();
                                            d = mIconManager.applyDataActivityColorFilter(d);
                                            mobileActivity.setImageDrawable(d);
                                        } catch (Resources.NotFoundException e) { 
                                            mobileActivity.setImageDrawable(null);
                                        }
                                    }
                                    ImageView mobileType = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileType");
                                    if (mobileType != null) {
                                        try {
                                            int resId = Utils.isMtkDevice() ?
                                                    (Integer) XposedHelpers.callMethod(mobileTypeId, "getIconId") :
                                                    XposedHelpers.getIntField(param.thisObject, "mMobileTypeId");
                                            Drawable d = res.getDrawable(resId).mutate();
                                            d = mIconManager.applyColorFilter(d);
                                            mobileType.setImageDrawable(d);
                                        } catch (Resources.NotFoundException e) { 
                                            mobileType.setImageDrawable(null);
                                        }
                                    }
                                    if (Utils.isMtkDevice() &&
                                            XposedHelpers.getBooleanField(param.thisObject, "mRoaming")) {
                                        ImageView mobileRoam = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileRoam");
                                        if (mobileRoam != null) {
                                            try {
                                                int resId = XposedHelpers.getIntField(param.thisObject, "mRoamingId");
                                                Drawable d = res.getDrawable(resId).mutate();
                                                d = mIconManager.applyColorFilter(d);
                                                mobileRoam.setImageDrawable(d);
                                            } catch (Resources.NotFoundException e) { 
                                                mobileRoam.setImageDrawable(null);
                                            }
                                        }
                                    }
                                }
                            }
    
                            // for SIM Slot 2
                            if (Utils.isMtkDevice() && 
                                    XposedHelpers.getBooleanField(param.thisObject, "mMobileVisibleGemini")) {
                                boolean allowChange = false;
                                ImageView mobile = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileGemini");
                                if (mobile != null) {
                                    try {
                                        int resId = (Integer) XposedHelpers.callMethod(mobileIconIdsGemini[0], "getIconId");
                                        String resName = res.getResourceEntryName(resId);
                                        allowChange = resName.contains("blue");
                                        Drawable d = mIconManager.getMobileIcon(resName);
                                        if (d != null) mobile.setImageDrawable(d);
                                    } catch (Resources.NotFoundException e) { }
                                }
                                if (allowChange) {
                                    ImageView mobileActivity = 
                                            (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileActivityGemini");
                                    if (mobileActivity != null) {
                                        try {
                                            int resId = (Integer) XposedHelpers.callMethod(mobileActivityIdGemini, "getIconId");
                                            Drawable d = res.getDrawable(resId).mutate();
                                            d = mIconManager.applyDataActivityColorFilter(d);
                                            mobileActivity.setImageDrawable(d);
                                        } catch (Resources.NotFoundException e) { 
                                            mobileActivity.setImageDrawable(null);
                                        }
                                    }
                                    ImageView mobileType = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileTypeGemini");
                                    if (mobileType != null) {
                                        try {
                                            int resId = (Integer) XposedHelpers.callMethod(mobileTypeIdGemini, "getIconId");
                                            Drawable d = res.getDrawable(resId).mutate();
                                            d = mIconManager.applyColorFilter(d);
                                            mobileType.setImageDrawable(d);
                                        } catch (Resources.NotFoundException e) { 
                                            mobileType.setImageDrawable(null);
                                        }
                                    }
                                    if (XposedHelpers.getBooleanField(param.thisObject, "mRoamingGemini")) {
                                        ImageView mobileRoam = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileRoamGemini");
                                        if (mobileRoam != null) {
                                            try {
                                                int resId = XposedHelpers.getIntField(param.thisObject, "mRoamingGeminiId");
                                                Drawable d = res.getDrawable(resId).mutate();
                                                d = mIconManager.applyColorFilter(d);
                                                mobileRoam.setImageDrawable(d);
                                            } catch (Resources.NotFoundException e) { 
                                                mobileRoam.setImageDrawable(null);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (Utils.isMtkDevice() && mRoamingIndicatorsDisabled) {
                        ImageView mobileRoam;
                        mobileRoam = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileRoam");
                        if (mobileRoam != null) mobileRoam.setVisibility(View.GONE);
                        mobileRoam = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mMobileRoamGemini");
                        if (mobileRoam != null) mobileRoam.setVisibility(View.GONE);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(notifPanelViewClass, "onFinishInflate", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mNotificationPanelView = (FrameLayout) param.thisObject;

                    mNotificationWallpaper = new NotificationWallpaper(mNotificationPanelView.getContext());
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT);
                    mNotificationWallpaper.setLayoutParams(lp);
                    mNotificationWallpaper.setType(prefs.getString(
                            GravityBoxSettings.PREF_KEY_NOTIF_BACKGROUND,
                            GravityBoxSettings.NOTIF_BG_DEFAULT));
                    mNotificationWallpaper.setColor(prefs.getInt(
                            GravityBoxSettings.PREF_KEY_NOTIF_COLOR, Color.BLACK));
                    mNotificationWallpaper.setColorMode(prefs.getString(
                            GravityBoxSettings.PREF_KEY_NOTIF_COLOR_MODE,
                            GravityBoxSettings.NOTIF_BG_COLOR_MODE_OVERLAY));
                    mNotificationWallpaper.setAlpha(prefs.getInt(
                            GravityBoxSettings.PREF_KEY_NOTIF_BACKGROUND_ALPHA, 60));
                    mNotificationPanelView.addView(mNotificationWallpaper);
                    updateNotificationPanelBackground();
                }
            });

        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void setStatusbarBgColor(int color) {
        if (mPanelBar == null) return;

        ColorDrawable colorDrawable = new ColorDrawable();
        colorDrawable.setColor(color);
        mPanelBar.setBackground(colorDrawable);
        log("statusbar background color set to: " + color);
    }

    private static void applyIconColors() {
        if (mSignalClusterView != null) {
            XposedHelpers.callMethod(mSignalClusterView, "apply");
        }

        final int iconColor = mIconColorEnabled ? 
                mIconManager.getIconColor() : StatusBarIconManager.DEFAULT_ICON_COLOR;

        if (mClock != null) {
            if (mClockDefaultColor == null) {
                mClockDefaultColor = mClock.getCurrentTextColor();
            }
            mClock.setTextColor(mIconColorEnabled ? iconColor : mClockDefaultColor);
        }

        if (mCircleBattery != null) {
            mCircleBattery.setColor(iconColor);
        }

        if (mPercentage != null) {
            if (mPercentageDefaultColor == null) {
                mPercentageDefaultColor = mPercentage.getCurrentTextColor();
            }
            mPercentage.setTextColor(mIconColorEnabled ? iconColor : mPercentageDefaultColor);
        }

        if (mBatteryController != null && mBattery != null) {
            Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
            intent.putExtra(BatteryManager.EXTRA_LEVEL, mBatteryLevel);
            intent.putExtra(BatteryManager.EXTRA_PLUGGED, mBatteryPlugged);
            XposedHelpers.callMethod(mBatteryController, "onReceive", mBattery.getContext(), intent);
        }
    }

    private static void updateNotificationPanelBackground() {
        if (mNotificationPanelView == null || mNotificationWallpaper == null) return;

        mNotificationPanelView.setBackgroundResource(0);
        mNotificationPanelView.setBackgroundResource(
                mNotificationPanelView.getResources().getIdentifier(
                        "notification_panel_bg", "drawable", PACKAGE_NAME));
        Drawable background = mNotificationPanelView.getBackground();
        float alpha = mNotificationWallpaper.getAlpha();
        background.setAlpha(alpha == 0 ? 255 : 
            (int)(1-alpha * 255));

        mNotificationWallpaper.updateNotificationWallpaper();
    }
}