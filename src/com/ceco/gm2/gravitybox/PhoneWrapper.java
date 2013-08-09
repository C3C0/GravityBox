package com.ceco.gm2.gravitybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PhoneWrapper {
    private static final String TAG = "PhoneWrapper";

    public static final int NT_WCDMA_PREFERRED = 0;
    public static final int NT_GSM_ONLY = 1;
    public static final int NT_WCDMA_ONLY = 2;
    public static final int NT_GSM_WCDMA_AUTO = 3;
    public static final int NT_LTE_CDMA_EVDO = 8; 
    public static final int NT_LTE_GSM_WCDMA = 9;
    public static final int NT_LTE_CMDA_EVDO_GSM_WCDMA = 10;
    public static final int NT_LTE_ONLY = 11;
    public static final int NT_LTE_WCDMA = 12;
    public static final int NT_MODE_UNKNOWN = 13;

    public static final String PREFERRED_NETWORK_MODE = "preferred_network_mode";

    public static final String ACTION_CHANGE_NETWORK_TYPE = "gravitybox.intent.action.CHANGE_NETWORK_TYPE";
    public static final String EXTRA_NETWORK_TYPE = "networkType";

    private static Class<?> mClsPhoneFactory;
    private static Class<?> mSystemProperties;
    private static Context mContext;

    private static void log(String msg) {
        XposedBridge.log(TAG + ": " + msg);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_CHANGE_NETWORK_TYPE) &&
                    intent.hasExtra(EXTRA_NETWORK_TYPE)) {
                int networkType = intent.getIntExtra(EXTRA_NETWORK_TYPE, NT_WCDMA_PREFERRED);
                log("received ACTION_CHANGE_NETWORK_TYPE broadcast: networkType = " + networkType);
                setPreferredNetworkType(networkType);
            }
        }
    };

    public static void initZygote() {
        log("Entering init state");

        try {
            mClsPhoneFactory = XposedHelpers.findClass("com.android.internal.telephony.PhoneFactory", null);
            mSystemProperties = XposedHelpers.findClass("android.os.SystemProperties", null);

            String methodName = Utils.isMtkDevice() ? "makeDefaultPhones" : "makeDefaultPhone";
            XposedHelpers.findAndHookMethod(mClsPhoneFactory, methodName, 
                    Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mContext = (Context) param.args[0];
                    log("PhoneFactory makeDefaultPhones - phone wrapper initialized");
                    onInitialize();
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void onInitialize() {
        if (mContext != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_CHANGE_NETWORK_TYPE);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    private static void setPreferredNetworkType(int networkType) {
        Object defPhone = XposedHelpers.callStaticMethod(mClsPhoneFactory, "getDefaultPhone");
        if (defPhone == null) return;

        try {
            if (Utils.isMtkDevice()) {
                Class<?>[] paramArgs = new Class<?>[3];
                paramArgs[0] = int.class;
                paramArgs[1] = Message.class;
                paramArgs[2] = int.class;
                XposedHelpers.callMethod(defPhone, "setPreferredNetworkTypeGemini", paramArgs, networkType, null, 0);
            } else {
                Settings.Global.putInt(mContext.getContentResolver(), PREFERRED_NETWORK_MODE, networkType);
                Class<?>[] paramArgs = new Class<?>[2];
                paramArgs[0] = int.class;
                paramArgs[1] = Message.class;
                XposedHelpers.callMethod(defPhone, "setPreferredNetworkType", paramArgs, networkType, null);
            }
        } catch (Exception e) {
            log("setPreferredNetworkType failed: " + e.getMessage());
            XposedBridge.log(e);
        }
    }

    public static int getDefaultNetworkType() {
        try {
            int mode = (Integer) XposedHelpers.callStaticMethod(mSystemProperties, 
                "getInt", "ro.telephony.default_network", NT_WCDMA_PREFERRED);
            log("getDefaultNetworkMode: mode=" + mode);
            return mode;
        } catch (Exception e) {
            XposedBridge.log(e);
            return NT_WCDMA_PREFERRED;
        }
    }
}