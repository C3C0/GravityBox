package com.ceco.gm2.gravitybox;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.ArrayList;

import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModBatteryStyle {
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    public static final String CLASS_BATTERY_CONTROLLER = "com.android.systemui.statusbar.policy.BatteryController";
    public static final String ACTION_BATTERY_STYLE_CHANGED = "mediatek.intent.action.BATTERY_PERCENTAGE_SWITCH";

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

                    Resources r = ((Context) XposedHelpers.getObjectField(param.thisObject, "mContext")).getResources();
                    ImageView circleBattery = (ImageView) mStatusBarView.findViewById(r.getIdentifier("circle_battery", "id", PACKAGE_NAME));

                    XposedHelpers.callMethod(mBatteryController, "addIconView", circleBattery);
                    XposedBridge.log("ModBatteryStyle: BatteryController.addIconView(circleBattery)");
                }
            });

            XposedBridge.hookAllConstructors(batteryControllerClass, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();
                    int mBatteryStyle = Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_BATTERY_STYLE, "1"));
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mBatteryStyle", mBatteryStyle);
                    XposedBridge.log("ModBatteryStyle: mBatteryStyle instance field injected; value = " + mBatteryStyle);
                }
            });

            findAndHookMethod(batteryControllerClass, "onReceive", Context.class, Intent.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    Intent intent = (Intent) param.args[1];

                    int mBatteryStyle = (Integer) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryStyle");

                    if (intent.getAction().equals(ACTION_BATTERY_STYLE_CHANGED) && intent.hasExtra("batteryStyle")) {
                        mBatteryStyle = intent.getIntExtra("batteryStyle", 1);
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mBatteryStyle", mBatteryStyle);
                        XposedBridge.log("ModBatteryStyle: mBatteryStyle changed to: " + mBatteryStyle);
                    }

                    @SuppressWarnings("unchecked")
                    ArrayList<ImageView> mIconViews = (ArrayList<ImageView>) XposedHelpers.getObjectField(param.thisObject, "mIconViews");

                    switch(mBatteryStyle) {
                        case GravityBoxSettings.BATTERY_STYLE_NONE:
                            mIconViews.get(0).setVisibility(View.GONE);
                            mIconViews.get(1).setVisibility(View.GONE);
                            break;
                        case GravityBoxSettings.BATTERY_STYLE_CIRCLE:
                            mIconViews.get(0).setVisibility(View.GONE);
                            mIconViews.get(1).setVisibility(View.VISIBLE);
                            break;
                        default:
                            mIconViews.get(0).setVisibility(View.VISIBLE);
                            mIconViews.get(1).setVisibility(View.GONE);                        
                    }
                }
            });
        }
        catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}