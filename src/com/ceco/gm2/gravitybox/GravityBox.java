package com.ceco.gm2.gravitybox;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GravityBox implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
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

        if (resparam.packageName.equals(ModBatteryStyle.PACKAGE_NAME))
            ModBatteryStyle.init(prefs, resparam);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals(ModLowBatteryWarning.PACKAGE_NAME))
            ModLowBatteryWarning.init(prefs, lpparam.classLoader);
    }
}