package com.ceco.gm2.gravitybox;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.ArrayList;

import android.content.Intent;
import android.content.Context;
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

public class ModBatteryStyle {
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    public static final String CLASS_BATTERY_CONTROLLER = "com.android.systemui.statusbar.policy.BatteryController";

    private static int mBatteryStyle;
    private static boolean mBatteryPercentText;

    public static void initResources(XSharedPreferences prefs, InitPackageResourcesParam resparam) {
        try {
            resparam.res.hookLayout(PACKAGE_NAME, "layout", "gemini_super_status_bar", new XC_LayoutInflated() {

                @Override
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {

                    ViewGroup vg = (ViewGroup) liparam.view.findViewById(
                            liparam.res.getIdentifier("signal_battery_cluster", "id", PACKAGE_NAME));

                    // GM2 specific - if there's already view with id "circle_battery", remove it
                    ImageView exView = (ImageView) vg.findViewById(liparam.res.getIdentifier("circle_battery", "id", PACKAGE_NAME));
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
                    vg.addView(circleBattery);
                    XposedBridge.log("ModBatteryStyle: CmCircleBattery injected");
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

            findAndHookMethod(phoneStatusBarClass, "makeStatusBarView", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    Object mBatteryController = XposedHelpers.getObjectField(param.thisObject, "mBatteryController");
                    View mStatusBarView = (View) XposedHelpers.getObjectField(param.thisObject, "mStatusBarView");
                    ImageView circleBattery = (ImageView) mStatusBarView.findViewWithTag("circle_battery");
                    XposedHelpers.callMethod(mBatteryController, "addIconView", circleBattery);
                    XposedBridge.log("ModBatteryStyle: BatteryController.addIconView(circleBattery)");
                }
            });

            XposedBridge.hookAllConstructors(batteryControllerClass, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    mBatteryStyle = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_BATTERY_STYLE, "1"));
                    mBatteryPercentText = prefs.getBoolean(GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT, false);
                    // handle obsolete settings
                    if (mBatteryStyle == 3 || mBatteryStyle == 4) {
                        mBatteryStyle = GravityBoxSettings.BATTERY_STYLE_STOCK;
                    }
                    XposedBridge.log("ModBatteryStyle: BatteryController constructed");
                }
            });

            findAndHookMethod(batteryControllerClass, "onReceive", Context.class, Intent.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    Intent intent = (Intent) param.args[1];

                    if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_BATTERY_STYLE_CHANGED)) {
                        if (intent.hasExtra("batteryStyle")) {
                            mBatteryStyle = intent.getIntExtra("batteryStyle", 1);
                            XposedBridge.log("ModBatteryStyle: mBatteryStyle changed to: " + mBatteryStyle);
                        }
                        if (intent.hasExtra("batteryPercent")) {
                            mBatteryPercentText = intent.getBooleanExtra("batteryPercent", false);
                            XposedBridge.log("ModBatteryStyle: mBatteryPercentText changed to: " + mBatteryPercentText);
                        }
                    }

                    @SuppressWarnings("unchecked")
                    ArrayList<ImageView> mIconViews = (ArrayList<ImageView>) XposedHelpers.getObjectField(param.thisObject, "mIconViews");
                    @SuppressWarnings("unchecked")
                    ArrayList<TextView> mLabelViews = (ArrayList<TextView>) XposedHelpers.getObjectField(param.thisObject, "mLabelViews");

                    mIconViews.get(0).setVisibility(
                            (mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_STOCK) ?
                                     View.VISIBLE : View.GONE);

                    mIconViews.get(1).setVisibility(mBatteryStyle == GravityBoxSettings.BATTERY_STYLE_CIRCLE ?
                            View.VISIBLE : View.GONE);

                    mLabelViews.get(0).setVisibility(
                            (mBatteryPercentText ? View.VISIBLE : View.GONE));
                }
            });
        }
        catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}