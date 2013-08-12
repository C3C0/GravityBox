package com.ceco.gm2.gravitybox;

import android.os.Build;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class GravityBox implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
    public static final String PACKAGE_NAME = GravityBox.class.getPackage().getName();
    public static String MODULE_PATH = null;
    private static XSharedPreferences prefs;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        prefs = new XSharedPreferences(PACKAGE_NAME);

        XposedBridge.log("Hardware: " + Build.HARDWARE);
        XposedBridge.log("Product: " + Build.PRODUCT);
        XposedBridge.log("Is MTK device: " + Utils.isMtkDevice());
        XposedBridge.log("Android SDK: " + Build.VERSION.SDK_INT);
        XposedBridge.log("Android Release: " + Build.VERSION.RELEASE);

        SystemWideResources.initResources(prefs);

        // MTK specific
        if (Utils.isMtkDevice()) {
            ModSignalIconHide.initZygote(prefs);

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_CALLER_ID_PHONE, false)) {
                FixCallerIdPhone.initZygote(prefs);
            }

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_DEV_OPTS, false)) {
                FixDevOptions.initZygote();
            }
        }

        // Common
        FixTraceFlood.initZygote();
        ModVolumeKeySkipTrack.init(prefs);
        ModVolKeyCursor.initZygote(prefs);
        ModCallCard.initZygote();
        ModStatusbarColor.initZygote();
        PhoneWrapper.initZygote();
        ModElectronBeam.initZygote(prefs);
        ModLockscreen.initZygote(prefs);
        ModLowBatteryWarning.initZygote(prefs);
        ModDisplay.initZygote(prefs);
        ModAudio.initZygote(prefs);
        ModHwKeys.initZygote(prefs);
        PatchMasterKey.initZygote();
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {

        if (resparam.packageName.equals(ModBatteryStyle.PACKAGE_NAME))
            ModBatteryStyle.initResources(prefs, resparam);

        if (resparam.packageName.equals(ModCenterClock.PACKAGE_NAME)) {
            ModCenterClock.initResources(prefs, resparam);
        }

        if (resparam.packageName.equals(FixDevOptions.PACKAGE_NAME)) {
            FixDevOptions.initPackageResources(prefs, resparam);
        }

        if (resparam.packageName.equals(ModQuickSettings.PACKAGE_NAME)) {
            ModQuickSettings.initResources(prefs, resparam);
        }

        if (resparam.packageName.equals(ModPieControls.PACKAGE_NAME)) {
            ModPieControls.initResources(prefs, resparam);
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        // MTK Specific
        if (Utils.isMtkDevice()) {
            if (lpparam.packageName.equals(ModSignalIconHide.PACKAGE_NAME)) {
                ModSignalIconHide.init(prefs, lpparam.classLoader);
            }

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_CALLER_ID_MMS, false) &&
                    lpparam.packageName.equals(FixCallerIdMms.PACKAGE_NAME)) {
                FixCallerIdMms.init(prefs, lpparam.classLoader);
            }

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_CALENDAR, false) &&
                    lpparam.packageName.equals(FixCalendar.PACKAGE_NAME)) {
                FixCalendar.init(prefs, lpparam.classLoader);
            }

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_DATETIME_CRASH, false) &&
                    lpparam.packageName.equals(FixDateTimeCrash.PACKAGE_NAME)) {
                FixDateTimeCrash.init(prefs, lpparam.classLoader);
            }

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_TTS_SETTINGS, false) &&
                    lpparam.packageName.equals(FixTtsSettings.PACKAGE_NAME)) {
                FixTtsSettings.init(prefs, lpparam.classLoader);
            }

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_DEV_OPTS, false) &&
                    lpparam.packageName.equals(FixDevOptions.PACKAGE_NAME)) {
                FixDevOptions.init(prefs, lpparam.classLoader);
            }

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_FIX_MMS_WAKELOCK, false) && 
                    lpparam.packageName.equals(FixMmsWakelock.PACKAGE_NAME)) {
                FixMmsWakelock.init(prefs, lpparam.classLoader);
            }

            if (lpparam.packageName.equals(ModAudioSettings.PACKAGE_NAME)) {
                ModAudioSettings.init(prefs, lpparam.classLoader);
            }
        }

        // Common
        if (lpparam.packageName.equals(ModBatteryStyle.PACKAGE_NAME)) {
            ModBatteryStyle.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModLowBatteryWarning.PACKAGE_NAME)) {
            ModLowBatteryWarning.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModClearAllRecents.PACKAGE_NAME)) {
            ModClearAllRecents.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModRebootMenu.PACKAGE_NAME)) {
            ModRebootMenu.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModCallCard.PACKAGE_NAME)) {
            ModCallCard.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModQuickSettings.PACKAGE_NAME)) {
            ModQuickSettings.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModStatusbarColor.PACKAGE_NAME)) {
            ModStatusbarColor.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModCenterClock.PACKAGE_NAME)) {
            ModCenterClock.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModPhone.PACKAGE_NAME)) {
            ModPhone.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModSettings.PACKAGE_NAME)) {
            ModSettings.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModVolumePanel.PACKAGE_NAME)) {
            ModVolumePanel.init(prefs, lpparam.classLoader);
        }

        if (lpparam.packageName.equals(ModPieControls.PACKAGE_NAME)) {
            ModPieControls.init(prefs, lpparam.classLoader);
        }
    }
}