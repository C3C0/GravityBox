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
        
        ModVolumeKeySkipTrack.init(prefs);
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {

        if (resparam.packageName.equals(ModBatteryStyle.PACKAGE_NAME))
            ModBatteryStyle.initResources(prefs, resparam);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals(ModBatteryStyle.PACKAGE_NAME))
            ModBatteryStyle.init(prefs, lpparam.classLoader);

        if (lpparam.packageName.equals(ModLowBatteryWarning.PACKAGE_NAME))
            ModLowBatteryWarning.init(prefs, lpparam.classLoader);

        if (lpparam.packageName.equals(ModSignalIconHide.PACKAGE_NAME))
            ModSignalIconHide.init(prefs, lpparam.classLoader);

        if (lpparam.packageName.equals(ModClearAllRecents.PACKAGE_NAME))
            ModClearAllRecents.init(lpparam.classLoader);
    }
}