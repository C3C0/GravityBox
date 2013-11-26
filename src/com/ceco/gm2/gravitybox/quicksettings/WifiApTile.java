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

import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.WifiManagerWrapper;
import com.ceco.gm2.gravitybox.WifiManagerWrapper.WifiApStateChangeListener;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class WifiApTile extends AQuickSettingsTile implements WifiApStateChangeListener {

    private WifiManagerWrapper mWifiManager;
    private int mWifiApState;
    private TextView mTextView;

    public WifiApTile(Context context, Context gbContext, Object statusBar,
            Object panelBar, WifiManagerWrapper wifiManager) {
        super(context, gbContext, statusBar, panelBar);

        mWifiManager = wifiManager;

        mOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mWifiApState != WifiManagerWrapper.WIFI_AP_STATE_ENABLED &&
                        mWifiApState != WifiManagerWrapper.WIFI_AP_STATE_DISABLED)
                    return;
                
                boolean enabled = (mWifiApState == WifiManagerWrapper.WIFI_AP_STATE_DISABLED);
                mWifiManager.setWifiApEnabled(enabled);
            }
        };

        mOnLongClick = new View.OnLongClickListener() {
            
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                startActivity(intent);
                return true;
            }
        };
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_wifi_ap, mTile);

        mTextView = (TextView) mTile.findViewById(R.id.wifi_ap_tileview);

        mWifiApState = mWifiManager.getWifiApState();
        mWifiManager.setWifiApStateChangeListener(this);
    }

    @Override
    protected synchronized void updateTile() {
        switch(mWifiApState) {
            case WifiManagerWrapper.WIFI_AP_STATE_ENABLED:
                mDrawableId = R.drawable.ic_qs_wifi_ap_on;
                mLabel = mGbResources.getString(R.string.quick_settings_wifi_ap_on);
                break;
            case WifiManagerWrapper.WIFI_AP_STATE_ENABLING:
            case WifiManagerWrapper.WIFI_AP_STATE_DISABLING:
                mDrawableId = R.drawable.ic_qs_wifi_ap_neutral;
                mLabel = "----";
                break;
            default:
                mDrawableId = R.drawable.ic_qs_wifi_ap_off;
                mLabel = mGbResources.getString(R.string.quick_settings_wifi_ap_off);
                break;
        }

        mTextView.setText(mLabel);
        if (mTileStyle == KITKAT) {
            Drawable d = mGbResources.getDrawable(mDrawableId).mutate();
            d.setColorFilter(mDrawableId == R.drawable.ic_qs_wifi_ap_off ? 
                    KK_COLOR_OFF : KK_COLOR_ON, PorterDuff.Mode.SRC_ATOP);
            mTextView.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        } else {
            mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
        }
    }

    @Override
    public void onWifiApStateChanged(int wifiApState) {
        mWifiApState = wifiApState;
        updateResources();
    }
}