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

package com.ceco.gm2.gravitybox.quicksettings;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.Utils;
import com.ceco.gm2.gravitybox.WifiManagerWrapper;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class WifiTile extends AQuickSettingsTile {
    private static final String TAG = "GB:WifiTile";
    private static final String CLASS_NCG_SIGNAL_CLUSTER = Utils.hasGeminiSupport() ?
            "com.android.systemui.statusbar.policy.NetworkControllerGemini.SignalCluster" :
            "com.android.systemui.statusbar.policy.NetworkController.SignalCluster";
    private static final boolean DEBUG = false;

    private WifiManagerWrapper mWifiManager;
    private TextView mTextView;
    private Map<String,Integer> mDrawableMap;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public WifiTile(Context context, Context gbContext, Object statusBar, Object panelBar,
            WifiManagerWrapper wifiManager) {
        super(context, gbContext, statusBar, panelBar);

        mWifiManager = wifiManager;
        prepareDrawableMap();

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWifiManager.toggleWifiEnabled();
            }
        };

        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(android.provider.Settings.ACTION_WIFI_SETTINGS);
                return true;
            }
        };
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_wifi, mTile);
        mTextView = (TextView) mTile.findViewById(R.id.wifi_tileview);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onTilePostCreate() {
        try {
            final Class<?> signalClusterClass =
                    XposedHelpers.findClass(CLASS_NCG_SIGNAL_CLUSTER, mContext.getClassLoader());
            final Object networkController = 
                    XposedHelpers.getObjectField(mStatusBar, Utils.hasGeminiSupport() ? 
                            "mNetworkControllerGemini" : "mNetworkController");
            final List<Object> signalClusters = 
                    (List<Object>) XposedHelpers.getObjectField(networkController, "mSignalClusters");
            signalClusters.add(Proxy.newProxyInstance(mContext.getClassLoader(),
                            new Class<?>[] { signalClusterClass }, new WifiSignalCluster()));
        } catch(Throwable t) {
            XposedBridge.log(t);
        }

        super.onTilePostCreate();
    }

    @Override
    protected synchronized void updateTile() {
        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }

    private void prepareDrawableMap() {
        mDrawableMap = new HashMap<String,Integer>();
        mDrawableMap.put("stat_sys_wifi_signal_0", R.drawable.ic_qs_wifi_0);
        mDrawableMap.put("stat_sys_wifi_signal_1", R.drawable.ic_qs_wifi_1);
        mDrawableMap.put("stat_sys_wifi_signal_2", R.drawable.ic_qs_wifi_2);
        mDrawableMap.put("stat_sys_wifi_signal_3", R.drawable.ic_qs_wifi_3);
        mDrawableMap.put("stat_sys_wifi_signal_4", R.drawable.ic_qs_wifi_4);
        mDrawableMap.put("stat_sys_wifi_signal_1_fully", R.drawable.ic_qs_wifi_full_1);
        mDrawableMap.put("stat_sys_wifi_signal_2_fully", R.drawable.ic_qs_wifi_full_2);
        mDrawableMap.put("stat_sys_wifi_signal_3_fully", R.drawable.ic_qs_wifi_full_3);
        mDrawableMap.put("stat_sys_wifi_signal_4_fully", R.drawable.ic_qs_wifi_full_4);
        mDrawableMap.put("stat_sys_wifi_signal_null", R.drawable.ic_qs_wifi_0);
    }

    private void updateResources(boolean connected, int iconId) {
        if (!connected && iconId == 0) {
            mDrawableId = R.drawable.ic_qs_wifi_off;
            mLabel = mGbResources.getString(R.string.quick_settings_wifi_off);
        } else {
            try {
                String resName = mResources.getResourceEntryName(iconId);
                mDrawableId = mDrawableMap.containsKey(resName) ?
                        mDrawableMap.get(resName) : R.drawable.ic_qs_wifi_0;
            } catch (NotFoundException e) {
                mDrawableId = R.drawable.ic_qs_wifi_0;
                XposedBridge.log(e);
            }
            mLabel = connected ? getWifiSsid() : 
                mGbResources.getString(R.string.quick_settings_wifi_not_connected);
        }

        updateResources();
    }

    private String getWifiSsid() {
        String ssid = mWifiManager.getWifiSsid();
        return (ssid == null ? 
                mGbResources.getString(R.string.quick_settings_wifi) :
                    ssid.substring(1, ssid.length()-1));
    }

    class WifiSignalCluster implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("setWifiIndicators")) {
                if (DEBUG) {
                    for (int i = 0; i < args.length; i++) {
                        log("setWifiIndicators: arg[" + i + "] = " + 
                                (args[i] == null ? "NULL" : args[i].toString())); 
                    }
                }
                updateResources((Boolean) args[0], (Integer) args[1]);
            }
            return null;
        }
    }
}
