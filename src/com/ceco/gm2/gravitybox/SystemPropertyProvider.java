package com.ceco.gm2.gravitybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ResultReceiver;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemPropertyProvider {
    private static final String TAG = "SystemPropertyProvider";
    public static final String PACKAGE_NAME = "com.android.systemui";

    public static final String ACTION_GET_SYSTEM_PROPERTIES = 
            "gravitybox.intent.action.ACTION_GET_SYSTEM_PROPERTIES";
    public static final int RESULT_SYSTEM_PROPERTIES = 1025;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final ClassLoader classLoader) {
        try {
            final Class<?> classSystemUIService = XposedHelpers.findClass(
                    "com.android.systemui.SystemUIService", classLoader);
            XposedHelpers.findAndHookMethod(classSystemUIService, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context context = (Context) param.thisObject;
                    if (context != null) {
                        log("SystemUIService created. Registering BroadcastReceiver");
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(ACTION_GET_SYSTEM_PROPERTIES);
                        context.registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                log("Broadcast received: " + intent.toString());
                                if (intent.getAction().equals(ACTION_GET_SYSTEM_PROPERTIES)
                                        && intent.hasExtra("receiver")) {
                                    ResultReceiver receiver = intent.getParcelableExtra("receiver");
                                    Bundle data = new Bundle();
                                    data.putBoolean("hasGeminiSupport", Utils.hasGeminiSupport());
                                    receiver.send(RESULT_SYSTEM_PROPERTIES, data);
                                }
                            }
                        }, intentFilter);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
