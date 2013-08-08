package com.ceco.gm2.gravitybox;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XCallback;

public class ModBatteryStyle {
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    public static final String CLASS_BATTERY_CONTROLLER = "com.android.systemui.statusbar.policy.BatteryController";

    private static int mBatteryStyle;
    private static boolean mBatteryPercentText;
    private static Object mBatteryController;

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_BATTERY_STYLE_CHANGED)) {
                if (intent.hasExtra("batteryStyle")) {
                    mBatteryStyle = intent.getIntExtra("batteryStyle", 1);
                    XposedBridge.log("ModBatteryStyle: mBatteryStyle changed to: " + mBatteryStyle);
                }
                if (intent.hasExtra("batteryPercent")) {
                    mBatteryPercentText = intent.getBooleanExtra("batteryPercent", false);
                    XposedBridge.log("ModBatteryStyle: mBatteryPercentText changed to: " + mBatteryPercentText);
                }
                updateBatteryStyle();
            }
        }
    };

    public static void initResources(XSharedPreferences prefs, InitPackageResourcesParam resparam) {
        try {
            String layout = Utils.isMtkDevice() ? "gemini_super_status_bar" : "super_status_bar";
            resparam.res.hookLayout(PACKAGE_NAME, "layout", layout, new XC_LayoutInflated() {

                @Override
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {

                    ViewGroup vg = (ViewGroup) liparam.view.findViewById(
                            liparam.res.getIdentifier("signal_battery_cluster", "id", PACKAGE_NAME));

                    // inject percent text if it doesn't exist
                    TextView percText = (TextView) vg.findViewById(liparam.res.getIdentifier(
                            "percentage", "id", PACKAGE_NAME));
                    if (percText == null) {
                        percText = new TextView(vg.getContext());
                        percText.setTag("percentage");
                        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                        percText.setLayoutParams(lParams);
                        percText.setPadding(4, 0, 0, 0);
                        percText.setTextSize(1, 16);
                        percText.setTextColor(StatusBarIconManager.DEFAULT_ICON_COLOR);
                        percText.setVisibility(View.GONE);
                        vg.addView(percText);
                        XposedBridge.log("ModBatteryStyle: Battery percent text injected");
                    }
                    ModStatusbarColor.setPercentage(percText);

                    // GM2 specific - if there's already view with id "circle_battery", remove it
                    ImageView exView = (ImageView) vg.findViewById(liparam.res.getIdentifier(
                            "circle_battery", "id", PACKAGE_NAME));
                    if (exView != null) {
                        XposedBridge.log("ModBatteryStyle: circle_battery view found - removing");
                        vg.removeView(exView);
                    }

                    // inject circle battery view
                    CmCircleBattery circleBattery = new CmCircleBattery(vg.getContext());
                    circleBattery.setTag("circle_battery");
                    LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    circleBattery.setLayoutParams(lParams);
                    circleBattery.setPadding(4, 0, 0, 0);
                    ModStatusbarColor.setCircleBattery(circleBattery);
                    vg.addView(circleBattery);
                    XposedBridge.log("ModBatteryStyle: CmCircleBattery injected");

                    // find battery
                    ImageView battery = (ImageView) vg.findViewById(
                            liparam.res.getIdentifier("battery", "id", PACKAGE_NAME));
                    ModStatusbarColor.setBattery(battery);
                }
                
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {

        XposedBridge.log("ModBatteryStyle: init");
        
        try {

            Class<?> phoneStatusBarClass = findClass(CLASS_PHONE_STATUSBAR, classLoader);
            Class<?> batteryControllerClass = findClass(CLASS_BATTERY_CONTROLLER, classLoader);

            findAndHookMethod(phoneStatusBarClass, "makeStatusBarView", new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    Object mBatteryController = XposedHelpers.getObjectField(param.thisObject, "mBatteryController");
                    View mStatusBarView = (View) XposedHelpers.getObjectField(param.thisObject, "mStatusBarView");

                    ImageView circleBattery = (ImageView) mStatusBarView.findViewWithTag("circle_battery");
                    if (circleBattery != null) {
                        XposedHelpers.callMethod(mBatteryController, "addIconView", circleBattery);
                        XposedBridge.log("ModBatteryStyle: BatteryController.addIconView(circleBattery)");
                    }

                    TextView percText = (TextView) mStatusBarView.findViewWithTag("percentage");
                    if (percText != null) {
                        // add percent text only in case there is no other label view present
                        // which might be another percent text coming from stock ROM with id other than "percentage"
                        @SuppressWarnings("unchecked")
                        ArrayList<TextView> mLabelViews = 
                            (ArrayList<TextView>) XposedHelpers.getObjectField(mBatteryController, "mLabelViews");
                        if (mLabelViews.isEmpty()) {
                            XposedHelpers.callMethod(mBatteryController, "addLabelView", percText);
                            XposedBridge.log("ModBatteryStyle: BatteryController.addLabelView(percText)");
                        }
                    }
                }
            });

            XposedBridge.hookAllConstructors(batteryControllerClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mBatteryController = param.thisObject;

                    prefs.reload();
                    mBatteryStyle = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_BATTERY_STYLE, "1"));
                    mBatteryPercentText = prefs.getBoolean(GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT, false);
                    // handle obsolete settings
                    if (mBatteryStyle == 4) {
                        mBatteryStyle = GravityBoxSettings.BATTERY_STYLE_STOCK;
                    }

                    Context context = (Context) XposedHelpers.getObjectField(mBatteryController, "mContext");
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_STYLE_CHANGED);
                    context.registerReceiver(mBroadcastReceiver, intentFilter);

                    XposedBridge.log("ModBatteryStyle: BatteryController constructed");
                }
            });

            findAndHookMethod(batteryControllerClass, "onReceive", Context.class, Intent.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    updateBatteryStyle();
                }
            });
        }
        catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void updateBatteryStyle() {
        if (mBatteryController == null) return;

        @SuppressWarnings("unchecked")
        ArrayList<ImageView> mIconViews = 
            (ArrayList<ImageView>) XposedHelpers.getObjectField(mBatteryController, "mIconViews");
        @SuppressWarnings("unchecked")
        ArrayList<TextView> mLabelViews = 
            (ArrayList<TextView>) XposedHelpers.getObjectField(mBatteryController, "mLabelViews");

        if (!mIconViews.isEmpty()) {
            mIconViews.get(0).setVisibility(
                    (mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_STOCK) ?
                             View.VISIBLE : View.GONE);

            if (mIconViews.size() >= 2) {
                ImageView iv = mIconViews.get(1);
                if (iv instanceof CmCircleBattery) {
                    iv.setVisibility((mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE ||
                            mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE_PERCENT) ?
                            View.VISIBLE : View.GONE);
                    ((CmCircleBattery)iv).setPercentage(
                            mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE_PERCENT);
                }
            }
        }

        if (!mLabelViews.isEmpty()) {
            mLabelViews.get(0).setVisibility(
                    (mBatteryPercentText ? View.VISIBLE : View.GONE));
        }
    }
}