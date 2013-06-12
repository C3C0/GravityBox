package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.GravityBox;
import com.ceco.gm2.gravitybox.WifiManagerWrapper;
import com.ceco.gm2.gravitybox.WifiManagerWrapper.WifiApStateChangeListener;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class WifiApTile extends AQuickSettingsTile implements WifiApStateChangeListener {

    private WifiManagerWrapper mWifiManager;
    private int mWifiApState;
    private TextView mTextView;

    public WifiApTile(Context context, Context gbContext, Object statusBar,
            Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mWifiManager = new WifiManagerWrapper(mContext);

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
        int mTileLayoutId = mGbResources.getIdentifier(
                "quick_settings_tile_wifi_ap", "layout", GravityBox.PACKAGE_NAME);
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(mTileLayoutId, mTile);

        mTextView = (TextView) mTile.findViewById(
                mGbResources.getIdentifier("wifi_ap_tileview", "id", GravityBox.PACKAGE_NAME));

        mWifiApState = mWifiManager.getWifiApState();
        mWifiManager.setWifiApStateChangeListener(this);
    }

    @Override
    protected synchronized void updateTile() {
        switch(mWifiApState) {
            case WifiManagerWrapper.WIFI_AP_STATE_ENABLED:
                mDrawableId = mGbResources.getIdentifier(
                        "ic_qs_wifi_ap_on", "drawable", GravityBox.PACKAGE_NAME);
                mLabel = mGbResources.getString(
                        mGbResources.getIdentifier(
                                "quick_settings_wifi_ap_on", "string", GravityBox.PACKAGE_NAME));
                break;
            case WifiManagerWrapper.WIFI_AP_STATE_ENABLING:
            case WifiManagerWrapper.WIFI_AP_STATE_DISABLING:
                mDrawableId = mGbResources.getIdentifier(
                        "ic_qs_wifi_ap_neutral", "drawable", GravityBox.PACKAGE_NAME);
                mLabel = "----";
                break;
            default:
                mDrawableId = mGbResources.getIdentifier("ic_qs_wifi_ap_off", "drawable", GravityBox.PACKAGE_NAME);
                mLabel = mGbResources.getString(
                        mGbResources.getIdentifier(
                                "quick_settings_wifi_ap_off", "string", GravityBox.PACKAGE_NAME));
                break;
        }

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }

    @Override
    public void onWifiApStateChanged(int wifiApState) {
        mWifiApState = wifiApState;
        updateResources();
    }
}