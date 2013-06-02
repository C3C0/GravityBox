package com.ceco.gm2.gravitybox;

import android.view.View;
import android.widget.ImageView;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class ModBatteryStyle {
    private static final String PACKAGE_NAME = "com.android.systemui";
    private static final String LAYOUT = "gemini_super_status_bar";

    public static void init(final XSharedPreferences prefs, InitPackageResourcesParam resparam) {

        if (!resparam.packageName.equals(PACKAGE_NAME))
            return;

        XposedBridge.log("ModBatteryStock: " + PACKAGE_NAME + " found");
        
        resparam.res.hookLayout(PACKAGE_NAME, "layout", LAYOUT, new XC_LayoutInflated() {

            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {

                boolean stockBattery = 
                        (Integer.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_BATTERY_STYLE, "0")) == 0);

                XposedBridge.log("ModBatteryStock: StatusBar inflated; stockBattery: " + stockBattery);

                if (stockBattery) {
                    ImageView batteryViewStock = (ImageView) liparam.view.findViewById(
                              liparam.res.getIdentifier("battery", "id", PACKAGE_NAME));
                    ImageView batteryViewCircle = (ImageView) liparam.view.findViewById(
                              liparam.res.getIdentifier("circle_battery", "id", PACKAGE_NAME));

                    batteryViewStock.setVisibility(View.VISIBLE);
                    batteryViewCircle.setVisibility(View.GONE);
                }
            }
        });
    }
}