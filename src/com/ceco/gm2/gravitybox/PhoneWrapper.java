/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.gm2.gravitybox;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Message;
import android.provider.Settings;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PhoneWrapper {
    private static final String TAG = "GB:PhoneWrapper";
    private static final boolean DEBUG = false;

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
    private static int mSimSlot;

    private static void log(String msg) {
        XposedBridge.log(TAG + ": " + msg);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_CHANGE_NETWORK_TYPE) &&
                    intent.hasExtra(EXTRA_NETWORK_TYPE)) {
                int networkType = intent.getIntExtra(EXTRA_NETWORK_TYPE, NT_WCDMA_PREFERRED);
                if (DEBUG) log("received ACTION_CHANGE_NETWORK_TYPE broadcast: networkType = " + networkType);
                setPreferredNetworkType(networkType);
            }
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_QS_NETWORK_MODE_SIM_SLOT_CHANGED)) {
                mSimSlot = intent.getIntExtra(GravityBoxSettings.EXTRA_SIM_SLOT, 0);
                if (DEBUG) log("received ACTION_PREF_QS_NETWORK_MODE_SIM_SLOT_CHANGED broadcast: " +
                                    "mSimSlot = " + mSimSlot);
            }
        }
    };

    public static void initZygote(final XSharedPreferences prefs) {
        if (DEBUG) log("Entering init state");

        try {
            mClsPhoneFactory = XposedHelpers.findClass("com.android.internal.telephony.PhoneFactory", null);
            mSystemProperties = XposedHelpers.findClass("android.os.SystemProperties", null);

            mSimSlot = 0;
            try {
                mSimSlot = Integer.valueOf(prefs.getString(
                        GravityBoxSettings.PREF_KEY_QS_NETWORK_MODE_SIM_SLOT, "0"));
            } catch (NumberFormatException nfe) {
                log("Invalid value for SIM Slot preference: " + nfe.getMessage());
            }
            if (DEBUG) log("mSimSlot = " + mSimSlot);

            String methodName = Utils.isMtkDevice() ? "makeDefaultPhones" : "makeDefaultPhone";
            XposedHelpers.findAndHookMethod(mClsPhoneFactory, methodName, 
                    Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mContext = (Context) param.args[0];
                    if (DEBUG) log("PhoneFactory makeDefaultPhones - phone wrapper initialized");
                    onInitialize();
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void onInitialize() {
        if (mContext != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_CHANGE_NETWORK_TYPE);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_QS_NETWORK_MODE_SIM_SLOT_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    @SuppressLint("NewApi")
    private static void setPreferredNetworkType(int networkType) {
        Object defPhone = XposedHelpers.callStaticMethod(mClsPhoneFactory, "getDefaultPhone");
        if (defPhone == null) return;

        try {
            if (Utils.hasGeminiSupport()) {
                Class<?>[] paramArgs = new Class<?>[3];
                paramArgs[0] = int.class;
                paramArgs[1] = Message.class;
                paramArgs[2] = int.class;
                XposedHelpers.callMethod(defPhone, "setPreferredNetworkTypeGemini", 
                        paramArgs, networkType, null, mSimSlot);
            } else {
                if (Build.VERSION.SDK_INT > 16) {
                    Settings.Global.putInt(mContext.getContentResolver(), PREFERRED_NETWORK_MODE, networkType);
                } else {
                    Settings.Secure.putInt(mContext.getContentResolver(), PREFERRED_NETWORK_MODE, networkType);
                }
                Class<?>[] paramArgs = new Class<?>[2];
                paramArgs[0] = int.class;
                paramArgs[1] = Message.class;
                XposedHelpers.callMethod(defPhone, "setPreferredNetworkType", paramArgs, networkType, null);
            }
        } catch (Throwable t) {
            log("setPreferredNetworkType failed: " + t.getMessage());
            XposedBridge.log(t);
        }
    }

    public static int getDefaultNetworkType() {
        try {
            int mode = (Integer) XposedHelpers.callStaticMethod(mSystemProperties, 
                "getInt", "ro.telephony.default_network", NT_WCDMA_PREFERRED);
            if (DEBUG) log("getDefaultNetworkMode: mode=" + mode);
            return mode;
        } catch (Throwable t) {
            XposedBridge.log(t);
            return NT_WCDMA_PREFERRED;
        }
    }
}