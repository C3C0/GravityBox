package com.ceco.gm2.gravitybox;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import de.robv.android.xposed.XSharedPreferences;

public class SystemWideResources {

    public static void initResources(final XSharedPreferences prefs) {
        XModuleResources modRes = XModuleResources.createInstance(GravityBox.MODULE_PATH, null);

        XResources.setSystemWideReplacement("android", "bool", "config_animateScreenLights", true);

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_DISABLE, false)) {
            XResources.setSystemWideReplacement("android", "bool", "config_showNavigationBar", false);
        }

        boolean holoBgDither = prefs.getBoolean(GravityBoxSettings.PREF_KEY_HOLO_BG_DITHER, false);
        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_HOLO_BG_SOLID_BLACK, false)) {
            XResources.setSystemWideReplacement(
                "android", "drawable", "background_holo_dark", modRes.fwd(R.drawable.background_holo_dark_solid));
        } else if (holoBgDither) {
            XResources.setSystemWideReplacement(
                    "android", "drawable", "background_holo_dark", modRes.fwd(R.drawable.background_holo_dark));
        }
        if (holoBgDither) {
            XResources.setSystemWideReplacement(
                    "android", "drawable", "background_holo_light", modRes.fwd(R.drawable.background_holo_light));
        }
    }

}
