package com.ceco.gm2.gravitybox;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GravityBox implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
    public static final String PACKAGE_NAME = GravityBox.class.getPackage().getName();
    private static XSharedPreferences prefs;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(PACKAGE_NAME);

        FixTraceFlood.initZygote();
        ModVolumeKeySkipTrack.init(prefs);
        ModSignalIconHide.initZygote(prefs);
        ModVolKeyCursor.initZygote(prefs);

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_CALLER_ID_PHONE, false))
            FixCallerIdPhone.initZygote(prefs);

        ModCallCard.initZygote();
        ModStatusbarColor.initZygote();

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_DEV_OPTS, false))
            FixDevOptions.initZygote();

        GeminiPhoneWrapper.initZygote();
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
            ModClearAllRecents.init(prefs, lpparam.classLoader);

        if (lpparam.packageName.equals(ModRebootMenu.PACKAGE_NAME))
            ModRebootMenu.init(prefs, lpparam.classLoader);

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_CALLER_ID_MMS, false) &&
                lpparam.packageName.equals(FixCallerIdMms.PACKAGE_NAME))
            FixCallerIdMms.init(prefs, lpparam.classLoader);

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_CALENDAR, false) &&
                lpparam.packageName.equals(FixCalendar.PACKAGE_NAME))
            FixCalendar.init(prefs, lpparam.classLoader);

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_DATETIME_CRASH, false) &&
                lpparam.packageName.equals(FixDateTimeCrash.PACKAGE_NAME)) {
            FixDateTimeCrash.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModCallCard.PACKAGE_NAME))
            ModCallCard.init(prefs, lpparam.classLoader);

        if (lpparam.packageName.equals(ModQuickSettings.PACKAGE_NAME))
            ModQuickSettings.init(prefs, lpparam.classLoader);

        if (lpparam.packageName.equals(ModStatusbarColor.PACKAGE_NAME))
            ModStatusbarColor.init(prefs, lpparam.classLoader);

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_TTS_SETTINGS, false) &&
                lpparam.packageName.equals(FixTtsSettings.PACKAGE_NAME)) {
            FixTtsSettings.init(prefs, lpparam.classLoader);
        }

        if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_DEV_OPTS, false) &&
                lpparam.packageName.equals(FixDevOptions.PACKAGE_NAME))
            FixDevOptions.init(prefs, lpparam.classLoader);
    }
}