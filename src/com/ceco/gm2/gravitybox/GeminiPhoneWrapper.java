package com.ceco.gm2.gravitybox;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class GeminiPhoneWrapper {
    private static final String TAG = "GeminiPhoneWrapper";

    public static final int EVENT_QUERY_NETWORKMODE_DONE = 0x65;
    public static final int EVENT_SET_NETWORKMODE_DONE = 0x66;

    public static final int NT_WCDMA_PREFERRED = 0;
    public static final int NT_GSM_ONLY = 1;
    public static final int NT_WCDMA_ONLY = 2;
    public static final int NT_GSM_WCDMA_AUTO = 3;

    public static final String PREFERRED_NETWORK_MODE = "preferred_network_mode";

    public static final String ACTION_CHANGE_NETWORK_TYPE = "gravitybox.intent.action.CHANGE_NETWORK_TYPE";
    public static final String EXTRA_NETWORK_TYPE = "networkType";

    private static Class<?> mClsPhoneFactory;
    private static Class<?> mClsPhoneGemini;
    private static Context mContext;
    private static boolean pendingInit = false;

    private static void log(String msg) {
        XposedBridge.log(TAG + ": " + msg);
    }

    public interface GeminiPhoneEventListener {
        void onInitialize(int networkType);
        void onNetworkTypeChanged(int networkType);
    }

    private static Handler mHandler;

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

    private static class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.Global.getUriFor(PREFERRED_NETWORK_MODE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            int nt = Settings.Global.getInt(mContext.getContentResolver(), 
                    PREFERRED_NETWORK_MODE, NT_WCDMA_PREFERRED);
            onNetworkTypeChanged(nt);
        }
    }

    private static ArrayList<GeminiPhoneEventListener> mListeners;

    public static void init(GeminiPhoneEventListener listener) {

        if (isInitialized() || pendingInit) {
            if (listener != null) {
                addEventListener(listener);
                if (!pendingInit) {
                    listener.onInitialize(getPreferredNetworkType());
                }
            }
            return;
        }

        log("Entering pending init state");
        pendingInit = true;
        mListeners = new ArrayList<GeminiPhoneEventListener>();
        addEventListener(listener);
        
        mClsPhoneFactory = XposedHelpers.findClass("com.android.internal.telephony.PhoneFactory", null);
        mClsPhoneGemini = XposedHelpers.findClass("com.android.internal.telephony.gemini.GeminiPhone", null);

        XposedBridge.hookAllConstructors(mClsPhoneGemini, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "f");
                log("GeminiPhone constructed. Leaving pending init state.");
                pendingInit = false;
                onInitialize();
            }
        });
    }

    private static void onInitialize() {
        mHandler = new Handler();

        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

        if (mContext != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_CHANGE_NETWORK_TYPE);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }

        int networkType = getPreferredNetworkType();
        log("onInitialize: preferredNetworkType = " + networkType);

        for (GeminiPhoneEventListener listener : mListeners) {
            listener.onInitialize(networkType);
        }
    }

    private static void onNetworkTypeChanged(int networkType) {
        log("onNetworkTypeChanged; networkType = " + networkType);
        if (mListeners == null) return;
        
        for (GeminiPhoneEventListener listener : mListeners) {
            listener.onNetworkTypeChanged(networkType);
        }
    }

    public static Context getContext() {
        return mContext;
    }

    public static boolean isInitialized() {
        return (mContext != null);
    }

    public static void addEventListener(GeminiPhoneEventListener listener) {
        if (mListeners == null || listener == null) return;

        mListeners.add(listener);
    }

    public static int getPreferredNetworkType() {
        int networkType = Settings.Global.getInt(mContext.getContentResolver(),
                PREFERRED_NETWORK_MODE, NT_WCDMA_PREFERRED);
        return networkType;
    }

    public static void setPreferredNetworkType(int networkType) {
        Object defPhone = XposedHelpers.callStaticMethod(mClsPhoneFactory, "getDefaultPhone");
        if (defPhone == null) return;

        Message msg = mHandler.obtainMessage(EVENT_SET_NETWORKMODE_DONE);
        XposedHelpers.callMethod(defPhone, "setPreferredNetworkTypeGemini", networkType, msg, 0);
    }
}