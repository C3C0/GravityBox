package com.ceco.gm2.gravitybox;

import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModSignalIconHide {
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String CLASS_SIGNAL_CLUSTER_VIEW = "com.android.systemui.statusbar.SignalClusterViewGemini";
    private static boolean autohideSlot1;
    private static boolean autohideSlot2;

    public static void init(final XSharedPreferences prefs, ClassLoader classLoader) {
        XposedBridge.log("ModSignalIconHide: init");

        try {

            Set<String> autoHidePrefs = prefs.getStringSet(GravityBoxSettings.PREF_KEY_SIGNAL_ICON_AUTOHIDE, null);
            autohideSlot1 = (autoHidePrefs != null && autoHidePrefs.contains("sim1"));
            autohideSlot2 = (autoHidePrefs != null && autoHidePrefs.contains("sim2"));
            XposedBridge.log("ModSignalIconHide: autohideSlot1 = " + autohideSlot1 + "; autohideSlot2 = " + autohideSlot2);

            Class<?> signalClusterViewClass = XposedHelpers.findClass(CLASS_SIGNAL_CLUSTER_VIEW, classLoader);

            XposedBridge.hookAllConstructors(signalClusterViewClass, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context context = ((LinearLayout) param.thisObject).getContext();
                    BroadcastReceiver br = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED)) {
                                XposedBridge.log("ModSignalIconHide: ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED broadcast received");
                                String[] autohidePrefs = intent.getStringArrayExtra("autohidePrefs");
                                autohideSlot1 = autohideSlot2 = false;
                                for (String str: autohidePrefs) {
                                    autohideSlot1 |= str.equals("sim1");
                                    autohideSlot2 |= str.equals("sim2");
                                }
                                XposedBridge.log("ModSignalIconHide: autohideSlot1 = " + autohideSlot1 + "; autohideSlot2 = " + autohideSlot2);
                                XposedBridge.log("ModSignalIconHide: invoking apply() method");
                                XposedHelpers.callMethod(param.thisObject, "apply");
                            }
                        }
                    };

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED);
                    context.registerReceiver(br, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(signalClusterViewClass, "apply", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ViewGroup vgSlot1 = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mMobileGroup");
                    ViewGroup vgSlot2 = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mMobileGroupGemini");

                    if (vgSlot1 != null) {
                        if (!(Boolean)XposedHelpers.callMethod(param.thisObject, "isSimInserted", 0) &&
                                autohideSlot1)
                            vgSlot1.setVisibility(View.GONE);
                        else
                            vgSlot1.setVisibility(View.VISIBLE);
                    }

                    if (vgSlot2 != null) { 
                        if (!(Boolean)XposedHelpers.callMethod(param.thisObject, "isSimInserted", 1) &&
                                autohideSlot2)
                            vgSlot2.setVisibility(View.GONE);
                        else
                            vgSlot2.setVisibility(View.VISIBLE);
                    }
                }
            });

        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}