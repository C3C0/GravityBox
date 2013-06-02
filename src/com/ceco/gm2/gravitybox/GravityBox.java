package com.ceco.gm2.gravitybox;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

public class GravityBox implements IXposedHookZygoteInit, IXposedHookInitPackageResources {
    private static final String PACKAGE_NAME = GravityBox.class.getPackage().getName();
    private static XSharedPreferences prefs;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(PACKAGE_NAME);

        if(prefs.getBoolean(GravityBoxSettings.PREF_KEY_VOL_MUSIC_CONTROLS, true))
            ModVolumeKeySkipTrack.init();
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        ModBatteryStyle.init(prefs, resparam);        
    }
}